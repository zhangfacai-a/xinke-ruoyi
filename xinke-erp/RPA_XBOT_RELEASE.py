"""影刀模块：正常结束或异常中止时释放当前批次。"""

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
TIMEOUT_SECONDS = 20
RETRIES = 2


def _required(args, name):
    value = args.get(name)
    if value is None or str(value).strip() == "":
        raise ValueError("缺少必填参数: " + name)
    return str(value).strip()


def _post_json(url, payload):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    headers = {"Accept": "application/json", "Content-Type": "application/json; charset=utf-8", "X-RPA-Key": RPA_KEY}
    for attempt in range(RETRIES + 1):
        try:
            req = request.Request(url=url, data=body, headers=headers, method="POST")
            with request.urlopen(req, timeout=TIMEOUT_SECONDS) as response:
                result = json.loads(response.read().decode("utf-8"))
            if result.get("code") != 200:
                raise RuntimeError("释放失败: code={0}, msg={1}".format(result.get("code"), result.get("msg") or "未知错误"))
            return result.get("data") or {}
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            if exc.code < 500 or attempt >= RETRIES:
                raise RuntimeError("释放请求失败: HTTP {0}, {1}".format(exc.code, detail[:500]))
        except (error.URLError, socket.timeout, TimeoutError) as exc:
            if attempt >= RETRIES:
                raise RuntimeError("无法连接服务器: " + str(exc))
        except json.JSONDecodeError:
            raise RuntimeError("服务器返回的不是有效JSON")
        wait_seconds = min(2 ** attempt, 5)
        print("释放接口暂时不可用，{0}秒后重试".format(wait_seconds))
        sleep(wait_seconds)


def main(args):
    args = args or {}
    payload = {
        "batchNo": _required(args, "batch_no"),
        "leaseToken": _required(args, "lease_token"),
        "workerId": WORKER_ID,
    }
    print("释放批次: " + payload["batchNo"])
    data = _post_json(BASE_URL.rstrip("/") + "/batch/release", payload)
    print("批次释放成功，释放任务数: {0}".format(data.get("releasedTaskCount", 0)))
    return data
