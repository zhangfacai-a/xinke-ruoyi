"""影刀模块：逐条回传查单和触达结果。"""

import json
import socket
import uuid
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
REQUEST_NAMESPACE = uuid.UUID("14780d6e-419d-4bc1-9317-30458c8d9a87")


def _required(args, name):
    value = args.get(name)
    if value is None or str(value).strip() == "":
        raise ValueError("缺少必填参数: " + name)
    return value


def _as_bool(value):
    if isinstance(value, bool):
        return value
    return str(value).strip().lower() in {"true", "1", "yes", "是"}


def _validate(payload):
    outcome = payload["outcome"]
    allowed = {"ORDERED", "CONTACTED", "SKIPPED", "FAILED", "RETRYABLE_ERROR"}
    if outcome not in allowed:
        raise ValueError("不支持的 outcome: " + outcome)
    if outcome == "ORDERED" and (not payload.get("douyinNo") or not payload.get("orderNo")):
        raise ValueError("ORDERED 必须提供 douyin_no 和 order_no")
    if outcome == "CONTACTED":
        if not payload.get("douyinNo"):
            raise ValueError("CONTACTED 必须提供 douyin_no")
        if not payload.get("followed") and not payload.get("messaged"):
            raise ValueError("CONTACTED 的 followed 或 messaged 至少一个必须为 True")
    if outcome in {"SKIPPED", "FAILED", "RETRYABLE_ERROR"}:
        if not payload.get("resultCode") and not payload.get("errorMessage"):
            raise ValueError(outcome + " 必须提供 result_code 或 error_message")


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
                raise RuntimeError("结果回传失败: code={0}, msg={1}".format(
                    result.get("code"), result.get("msg") or "未知错误"))
            return result.get("data") or {}
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            if exc.code < 500 or attempt >= retries:
                raise RuntimeError("回传请求失败: HTTP {0}, {1}".format(exc.code, detail[:500]))
        except (error.URLError, socket.timeout, TimeoutError) as exc:
            if attempt >= retries:
                raise RuntimeError("无法连接服务器: " + str(exc))
        except json.JSONDecodeError:
            raise RuntimeError("服务器返回的不是有效JSON")

        wait_seconds = min(2 ** attempt, 5)
        print("回传接口暂时不可用，{0}秒后重试".format(wait_seconds))
        sleep(wait_seconds)


def main(args):
    args = args or {}
    api_key = RPA_KEY
    batch_no = str(_required(args, "batch_no")).strip()
    task_no = str(_required(args, "task_no")).strip()
    outcome = str(_required(args, "outcome")).strip().upper()

    # 相同批次和任务始终生成同一个编号，影刀失败重跑时不会重复入账。
    request_id = str(args.get("request_id") or uuid.uuid5(
        REQUEST_NAMESPACE, batch_no + "|" + task_no))
    payload = {
        "batchNo": batch_no,
        "leaseToken": str(_required(args, "lease_token")).strip(),
        "workerId": WORKER_ID,
        "requestId": request_id,
        "taskNo": task_no,
        "outcome": outcome,
    }
    optional_text = {
        "douyin_no": "douyinNo",
        "order_no": "orderNo",
        "message_content": "messageContent",
        "result_code": "resultCode",
        "error_message": "errorMessage",
    }
    for input_name, api_name in optional_text.items():
        value = args.get(input_name)
        if value is not None and str(value).strip() != "":
            payload[api_name] = str(value).strip()
    if "followed" in args:
        payload["followed"] = _as_bool(args.get("followed"))
    if "messaged" in args:
        payload["messaged"] = _as_bool(args.get("messaged"))

    _validate(payload)
    base_url = BASE_URL.rstrip("/")
    timeout_seconds = TIMEOUT_SECONDS
    retries = RETRIES
    print("开始回传任务结果: " + task_no)
    data = _post_json(base_url + "/task/result", api_key, payload, timeout_seconds, retries)
    data["requestId"] = request_id
    print("任务结果回传成功: " + task_no)
    return data
