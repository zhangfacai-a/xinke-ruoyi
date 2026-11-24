"""Build the local app icon and self-contained user guide shipped by the installer."""
from pathlib import Path
import html
import re
import json
import hashlib
import xml.etree.ElementTree as ET
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / 'clients/anchor-metrics-probe'
VERSION = ET.parse(APP / 'AnchorMetricsProbe.csproj').findtext('./PropertyGroup/Version')
size = 256
image = Image.new('RGBA', (size, size), (0, 0, 0, 0))
draw = ImageDraw.Draw(image)
draw.rounded_rectangle((4, 4, 252, 252), radius=54, fill='#e86023')
font = ImageFont.truetype('C:/Windows/Fonts/msyhbd.ttc', 164)
box = draw.textbbox((0, 0), '鑫', font=font)
draw.text(((size-(box[2]-box[0]))/2-box[0], (size-(box[3]-box[1]))/2-box[1]-5), '鑫', font=font, fill='white')
image.save(APP / 'app.ico', format='ICO', sizes=[(s,s) for s in (16,24,32,48,64,128,256)])

source = (ROOT / 'docs/anchor-workbench-user-guide.md').read_text(encoding='utf-8')
blocks = []
list_type = None
def close_list():
    global list_type
    if list_type:
        blocks.append(f'</{list_type}>')
        list_type = None
for line in source.splitlines():
    if not line.strip():
        close_list()
        continue
    heading = re.match(r'^(#{1,3}) (.*)', line)
    item = re.match(r'^(?:- |\d+\. )(.*)', line)
    if heading:
        close_list()
        level = len(heading[1]); blocks.append(f'<h{level}>{html.escape(heading[2])}</h{level}>')
    elif item:
        kind = 'ul' if line.startswith('- ') else 'ol'
        if kind != list_type:
            close_list(); blocks.append(f'<{kind}>'); list_type = kind
        blocks.append('<li>'+html.escape(item[1])+'</li>')
    else:
        close_list(); blocks.append('<p>'+html.escape(line)+'</p>')
close_list()
help_dir = APP / 'help'
help_dir.mkdir(exist_ok=True)
document = '''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>鑫客主播工作台 · 使用说明</title><style>
*{box-sizing:border-box}body{font:15px/1.9 'Microsoft YaHei UI','Microsoft YaHei',sans-serif;color:#283547;background:#f4f6f8;margin:0;padding:32px 18px}main{max-width:920px;margin:auto;background:white;padding:38px 48px;border:1px solid #e4e9ef;border-radius:10px}h1{font-size:27px;margin:0 0 8px}h2{font-size:20px;border-top:1px solid #e5e9ef;padding-top:25px;margin-top:30px}h3{font-size:17px}p{margin:12px 0;color:#536174}li{margin:9px 0}h1:before{content:'鑫';display:inline-grid;place-items:center;background:#e86023;color:white;border-radius:8px;width:38px;height:38px;margin-right:12px;font-size:25px}footer{font-size:12px;color:#758294;border-top:1px solid #e5e9ef;margin-top:30px;padding-top:15px}@media(max-width:600px){main{padding:24px 20px}h1{font-size:23px}}@media print{body{background:white;padding:0}main{border:0;padding:0}h2{break-after:avoid}li,p{orphans:3;widows:3}}
</style></head><body><main>'''+ '\n'.join(blocks) + f'<footer>鑫客主播工作台 {VERSION} · 本文档可离线阅读，也可使用浏览器打印功能保存为 PDF。</footer></main></body></html>'
(help_dir / '使用说明.html').write_text(document, encoding='utf-8-sig')
(help_dir / '安装后须知.txt').write_text(f'鑫客主播工作台 {VERSION}\n\n请使用自己的鑫客网站账号和百应账号登录。\n选择实际上播主播 → 采集并预览 → 核对区间 → 确认上报。\n\n开机自启只在系统托盘启动，不会自动提交数据；可在设置中关闭。\n已运行时再次打开会显示原窗口。最小化进入托盘，关闭按钮退出。\n\n完整说明可从App设置或开始菜单打开，无需联网。\n', encoding='utf-8-sig')
files = [APP/'app.ico', *sorted((APP/'ui').rglob('*')), *sorted(help_dir.rglob('*'))]
manifest = {'version': VERSION, 'files': {p.relative_to(APP).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in files if p.is_file()}}
assert 'ui/anchor-desktop.html' in manifest['files'], 'Build the desktop UI first'
(APP/'desktop-assets.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Generated icon, offline guide and {len(manifest["files"])} asset hashes for {VERSION}.')
