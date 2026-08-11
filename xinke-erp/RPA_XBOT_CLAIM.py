"""影刀模块：领取一批同店铺的追单任务。"""

import json
import socket
from urllib import error, request

import xbot
from xbot import print, sleep
from . import package
from .package import variables as glv


BASE_URL = "http://localhost:8080/open-api/rpa/outreach"
RPA_KEY = "test-rpa-key-2026"
WORKER_ID = "pc-01"
CLAIM_LIMIT = 10
TIMEOUT_SECONDS = 20
RETRIES = 2


def _required(args, name):
    value = args.get(name)
    if value is None or str(value).strip() == "":
        raise ValueError("缺少必填参数: " + name)
    return value


def _post_json(url, api_key, payload, timeout_seconds, retries):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {
        "Accept": "application/json",
        "Content-Type": "application/json; charset=utf-8",
        "X-RPA-Key": api_key,
    }
    for attempt in range(retries + 1):
        try:
            req = request.Request(url=url, data=body, headers=headers, method="POST")
            with request.urlopen(req, timeout=timeout_seconds) as response:
                result = json.loads(response.read().decode("utf-8"))
            if result.get("code") != 200:
                raise RuntimeError("领取失败: code={0}, msg={1}".format(
                    result.get("code"), result.get("msg") or "未知错误"))
            return result.get("data") or {}
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            if exc.code < 500 or attempt >= retries:
                raise RuntimeError("领取请求失败: HTTP {0}, {1}".format(exc.code, detail[:500]))
        except (error.URLError, socket.timeout, TimeoutError) as exc:
            if attempt >= retries:
                raise RuntimeError("无法连接服务器: " + str(exc))
        except json.JSONDecodeError:
            raise RuntimeError("服务器返回的不是有效JSON")

        wait_seconds = min(2 ** attempt, 5)
        print("领取接口暂时不可用，{0}秒后重试".format(wait_seconds))
        sleep(wait_seconds)


def main(args):
    args = args or {}
    api_key = RPA_KEY
    worker_id = WORKER_ID
    limit = CLAIM_LIMIT
    if limit < 1 or limit > 10:
        raise ValueError("limit 必须在1到10之间")

    payload = {"workerId": worker_id, "limit": limit}
    preferred_shop_code = args.get("preferred_shop_code")
    if preferred_shop_code:
        payload["preferredShopCode"] = str(preferred_shop_code).strip()

    base_url = BASE_URL.rstrip("/")
    timeout_seconds = TIMEOUT_SECONDS
    retries = RETRIES
    print("开始领取RPA任务")
    data = _post_json(base_url + "/task/claim", api_key, payload, timeout_seconds, retries)
    if data.get("available"):
        batch = data.get("batch") or {}
        tasks = data.get("tasks") or []
        print("领取成功，批次: {0}，店铺: {1}，任务数: {2}".format(
            batch.get("batchNo") or "-", batch.get("shopName") or "-", len(tasks)))
        print("本批租约秒数: {0}".format(data.get("leaseSeconds") or 0))
    else:
        print("当前无任务，建议{0}秒后重试".format(data.get("retryAfterSeconds") or 60))
    return data
