"""影刀模块：检查追单服务是否可用。所有连接参数直接写在文件中。"""

import json
import socket
from urllib import error, request

import xbot
from xbot import print, sleep
from . import package
from .package import variables as glv


BASE_URL = "http://localhost:8080/open-api/rpa/outreach"
RPA_KEY = "test-rpa-key-2026"
TIMEOUT_SECONDS = 20
RETRIES = 2


def _get_json(url, timeout_seconds, retries):
    headers = {"Accept": "application/json", "X-RPA-Key": RPA_KEY}
    for attempt in range(retries + 1):
        try:
            req = request.Request(url=url, headers=headers, method="GET")
            with request.urlopen(req, timeout=timeout_seconds) as response:
                result = json.loads(response.read().decode("utf-8"))
            if result.get("code") != 200:
                raise RuntimeError("健康检查失败: code={0}, msg={1}".format(
                    result.get("code"), result.get("msg") or "未知错误"))
            return result.get("data") or {}
        except error.HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            if exc.code < 500 or attempt >= retries:
                raise RuntimeError("健康检查请求失败: HTTP {0}, {1}".format(exc.code, detail[:500]))
        except (error.URLError, socket.timeout, TimeoutError) as exc:
            if attempt >= retries:
                raise RuntimeError("无法连接服务器: " + str(exc))
        except json.JSONDecodeError:
            raise RuntimeError("服务器返回的不是有效JSON")
        wait_seconds = min(2 ** attempt, 5)
        print("健康检查暂时不可用，{0}秒后重试".format(wait_seconds))
        sleep(wait_seconds)


def main(args):
    print("检查追单服务器")
    data = _get_json(BASE_URL.rstrip("/") + "/health", TIMEOUT_SECONDS, RETRIES)
    if not data.get("ready"):
        raise RuntimeError("服务器已连接，但追单服务尚未准备完成")
    print("服务器正常，可领取任务，当前店铺数: {0}".format(data.get("configuredShopCount", 0)))
    return data
