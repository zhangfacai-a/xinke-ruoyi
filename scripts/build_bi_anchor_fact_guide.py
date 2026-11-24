from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Inches, Pt, RGBColor


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "主播直播区间事实表使用说明.docx"


def set_cell_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = tc_pr.find(qn("w:shd"))
    if shd is None:
        shd = OxmlElement("w:shd")
        tc_pr.append(shd)
    shd.set(qn("w:fill"), fill)


def set_cell_margins(cell, top=90, start=100, bottom=90, end=100):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for margin, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{margin}"))
        if node is None:
            node = OxmlElement(f"w:{margin}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_repeat_table_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    tbl_header = OxmlElement("w:tblHeader")
    tbl_header.set(qn("w:val"), "true")
    tr_pr.append(tbl_header)


def prevent_row_split(row):
    tr_pr = row._tr.get_or_add_trPr()
    cant_split = OxmlElement("w:cantSplit")
    tr_pr.append(cant_split)


def set_table_borders(table, color="D9D9D9", size="6"):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.first_child_found_in("w:tblBorders")
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        tag = f"w:{edge}"
        node = borders.find(qn(tag))
        if node is None:
            node = OxmlElement(tag)
            borders.append(node)
        node.set(qn("w:val"), "single")
        node.set(qn("w:sz"), size)
        node.set(qn("w:color"), color)


def set_run_font(run, name="Microsoft YaHei", size=None, bold=None, color=None):
    run.font.name = name
    run._element.get_or_add_rPr().get_or_add_rFonts().set(qn("w:eastAsia"), name)
    run._element.rPr.rFonts.set(qn("w:ascii"), name)
    run._element.rPr.rFonts.set(qn("w:hAnsi"), name)
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if color:
        run.font.color.rgb = RGBColor.from_string(color)


def add_heading(doc, text, level=1):
    p = doc.add_heading(text, level=level)
    p.paragraph_format.space_before = Pt(10 if level == 1 else 7)
    p.paragraph_format.space_after = Pt(5)
    p.paragraph_format.keep_with_next = True
    for run in p.runs:
        set_run_font(run, size=15 if level == 1 else 12, bold=True, color="000000")
    return p


def add_body(doc, text, bold_lead=None):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(5)
    p.paragraph_format.line_spacing = 1.18
    if bold_lead and text.startswith(bold_lead):
        lead = p.add_run(bold_lead)
        set_run_font(lead, size=10.5, bold=True)
        rest = p.add_run(text[len(bold_lead):])
        set_run_font(rest, size=10.5)
    else:
        run = p.add_run(text)
        set_run_font(run, size=10.5)
    return p


def add_bullet(doc, text):
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.left_indent = Cm(0.65)
    p.paragraph_format.first_line_indent = Cm(-0.25)
    set_run_font(p.add_run(text), size=10.2)
    return p


def add_code(doc, code):
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(3)
    p.paragraph_format.space_after = Pt(8)
    p.paragraph_format.left_indent = Cm(0.35)
    p.paragraph_format.right_indent = Cm(0.35)
    p.paragraph_format.line_spacing = 1.0
    p_pr = p._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), "F4F6F8")
    p_pr.append(shd)
    run = p.add_run(code)
    set_run_font(run, name="Consolas", size=8.3, color="202124")
    return p


def add_table(doc, headers, rows, widths, font_size=8.8):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)
    header = table.rows[0]
    set_repeat_table_header(header)
    for idx, (cell, title) in enumerate(zip(header.cells, headers)):
        cell.width = widths[idx]
        cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
        set_cell_margins(cell)
        set_cell_shading(cell, "2F5597")
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(0)
        run = p.add_run(title)
        set_run_font(run, size=9, bold=True, color="FFFFFF")
    for row_index, values in enumerate(rows):
        row = table.add_row()
        prevent_row_split(row)
        if row_index % 2:
            for cell in row.cells:
                set_cell_shading(cell, "F7F9FC")
        for idx, (cell, value) in enumerate(zip(row.cells, values)):
            cell.width = widths[idx]
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_cell_margins(cell)
            p = cell.paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if idx in (0, 1, 2) else WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.05
            run = p.add_run(str(value))
            set_run_font(run, size=font_size)
    doc.add_paragraph().paragraph_format.space_after = Pt(1)
    return table


def add_footer(section):
    footer = section.footer
    table = footer.add_table(rows=1, cols=2, width=Inches(7.0))
    table.columns[0].width = Inches(5.7)
    table.columns[1].width = Inches(1.3)
    left = table.cell(0, 0).paragraphs[0]
    set_run_font(left.add_run("主播直播区间事实表使用说明  2026-11-03"), size=8, color="666666")
    right = table.cell(0, 1).paragraphs[0]
    right.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    set_run_font(right.add_run("第 "), size=8, color="666666")
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), "PAGE")
    right._p.append(fld)
    set_run_font(right.add_run(" 页"), size=8, color="666666")


def field_rows():
    return {
        "标识和时间字段": [
            ("segment_id", "BIGINT", "10086", "主播区间记录ID，主键；来自 live_metric_anchor_segment.segment_id。"),
            ("session_id", "BIGINT", "501", "直播场次内部ID，用于关联同一场直播的多个主播区间。"),
            ("platform_session_id", "VARCHAR(64)", "7701099...", "平台直播场次ID，即抖音 live_room_id。"),
            ("business_date", "DATE", "2026-11-03", "业务归属日期，固定取本场直播开播日期；跨午夜仍归属开播日。"),
            ("room_id", "BIGINT", "28", "直播间内部ID，关联 live_room.room_id。"),
            ("room_name", "VARCHAR(160)", "一号直播间", "直播间名称快照，后续改名不会改变历史口径。"),
            ("anchor_user_id", "BIGINT", "1024", "主播用户ID，关联 sys_user.user_id。"),
            ("anchor_name", "VARCHAR(100)", "示例主播甲", "主播名称快照，适合报表直接展示。"),
            ("session_live_start_at", "DATETIME", "2026-11-03 06:00:00", "整场直播的开播时间。"),
            ("segment_start_at", "DATETIME", "2026-11-03 09:00:00", "该主播负责区间的开始时间。"),
            ("segment_end_at", "DATETIME", "2026-11-03 12:00:00", "该主播负责区间的结束时间。"),
            ("duration_seconds", "INT UNSIGNED", "10800", "主播负责区间时长，单位秒。"),
        ],
        "业务指标字段": [
            ("watch_count", "BIGINT", "1363", "区间新增观看人数，由累计快照差值计算。"),
            ("interaction_count", "BIGINT", "113", "区间新增互动人数，由累计快照差值计算。"),
            ("new_fan_count", "BIGINT", "40", "区间新增粉丝数，由累计快照差值计算。"),
            ("deal_count", "BIGINT", "17", "区间新增成交人数，由累计快照差值计算。"),
            ("gmv_amount", "DECIMAL(18,4)", "57311.0000", "区间成交金额，单位元。"),
            ("ad_cost_amount", "DECIMAL(18,4)", "1353.0900", "区间千川消耗金额，单位元。"),
            ("presale_order_count", "BIGINT", "0", "区间新增预售订单数。"),
            ("exposure_count_est", "DECIMAL(20,4)", "33990.0249", "估算曝光次数；无法估算时为 NULL。"),
            ("exposure_watch_rate", "DECIMAL(10,4)", "4.0100", "区间曝光-观看率，单位百分比。聚合时禁止直接平均。"),
            ("watch_follow_rate", "DECIMAL(10,4)", "2.9347", "区间观看-关注率，单位百分比。聚合时禁止直接平均。"),
            ("watch_interaction_rate", "DECIMAL(10,4)", "8.2905", "区间观看-互动率，单位百分比。聚合时禁止直接平均。"),
            ("watch_deal_rate", "DECIMAL(10,4)", "1.2472", "区间观看-成交率，单位百分比。聚合时禁止直接平均。"),
        ],
        "同步和质量字段": [
            ("source_type", "VARCHAR(20)", "PLUGIN", "数据来源类型，例如插件上报或手工补录。"),
            ("precision_status", "VARCHAR(20)", "EXACT", "区间精度状态；EXACT 表示开始和结束边界精确。"),
            ("quality_status", "VARCHAR(16)", "VALID", "质量等级：VALID、WARNING、INVALID。"),
            ("quality_flags", "VARCHAR(500)", "SESSION_STALE", "质量标记，多项用英文逗号分隔；无异常时为 NULL。"),
            ("source_version", "INT", "3", "源记录版本号，用于识别增量变更。"),
            ("source_created_at", "DATETIME", "2026-11-03 12:00:04", "源记录创建时间。"),
            ("source_updated_at", "DATETIME", "2026-11-03 12:05:18", "源记录最后更新时间。"),
            ("synced_at", "DATETIME", "2026-11-03 12:05:19", "事实表最近同步时间。"),
            ("is_deleted", "TINYINT(1)", "0", "软删除标记：0有效；1表示源记录已删除、作废或未完成。"),
            ("row_hash", "CHAR(64)", "a7f9...", "同步字段的 SHA-256 哈希，用于一致性核验。"),
        ],
    }


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    doc = Document()
    section = doc.sections[0]
    section.page_width = Cm(21)
    section.page_height = Cm(29.7)
    section.top_margin = Cm(1.55)
    section.bottom_margin = Cm(1.55)
    section.left_margin = Cm(1.7)
    section.right_margin = Cm(1.7)
    add_footer(section)

    styles = doc.styles
    styles["Normal"].font.name = "Microsoft YaHei"
    styles["Normal"]._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    styles["Normal"].font.size = Pt(10.5)
    for style_name in ("Title", "Heading 1", "Heading 2"):
        style = styles[style_name]
        style.font.name = "Microsoft YaHei"
        style._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
        style.font.color.rgb = RGBColor(0, 0, 0)

    title = doc.add_paragraph(style="Title")
    title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    title.paragraph_format.space_after = Pt(8)
    set_run_font(title.add_run("主播直播区间事实表使用说明"), size=22, bold=True, color="000000")
    subtitle = doc.add_paragraph()
    subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
    subtitle.paragraph_format.space_after = Pt(18)
    set_run_font(subtitle.add_run("bi_live_anchor_segment_fact  数据交接与 BI 查询指南"), size=10.5, color="555555")

    add_body(doc, "本文档用于向数据分析、BI 开发和业务管理人员说明主播直播区间事实表的业务口径与查询方式。该表已经完成历史回填和源表对账，可作为主播区间分析的统一明细数据源。")

    add_heading(doc, "一 快速结论", 1)
    quick_rows = [
        ("数据粒度", "一行代表一名主播在一场直播中的一个负责区间"),
        ("主键", "segment_id"),
        ("有效数据条件", "is_deleted = 0"),
        ("推荐质量条件", "quality_status IN ('VALID','WARNING')；INVALID 不进入正式指标"),
        ("业务日期", "business_date，取整场直播开播日，跨午夜不切日"),
        ("更新方式", "业务提交后实时同步，并每 5 分钟兜底同步"),
        ("百分比口径", "禁止 AVG(rate)；必须使用汇总后的分子和分母重新计算"),
    ]
    add_table(doc, ["项目", "使用规则"], quick_rows, [Cm(4.1), Cm(13.0)], font_size=9.4)

    add_heading(doc, "二 使用前必须遵守的规则", 1)
    add_bullet(doc, "所有正式查询都应包含 is_deleted = 0，避免已作废或未完成记录进入统计。")
    add_bullet(doc, "VALID 可直接使用；WARNING 可用于业务统计，但应保留 quality_flags 供核查；INVALID 应排除。")
    add_bullet(doc, "金额字段单位为元，时长字段单位为秒，四个 rate 字段的数值单位为百分比。")
    add_bullet(doc, "room_name 和 anchor_name 是历史快照。需要当前名称时，应另行关联维表。")
    add_bullet(doc, "百分比字段只能用于单行核对。跨主播、跨直播间或跨日期汇总时必须重新计算。")

    doc.add_page_break()
    add_heading(doc, "三 字段字典", 1)
    add_body(doc, "示例值仅用于解释格式，不代表生产数据。")
    for group, rows in field_rows().items():
        add_heading(doc, group, 2)
        add_table(doc, ["字段", "类型", "示例", "业务含义"], rows, [Cm(4.0), Cm(3.0), Cm(3.1), Cm(7.0)], font_size=8.4)

    add_heading(doc, "四 百分比聚合口径", 1)
    add_body(doc, "四个比率字段保存的是单个主播区间的核对值。多个区间合并时，直接使用 AVG 会让小流量区间和大流量区间获得相同权重，造成看板失真。正确公式如下。")
    rate_rows = [
        ("曝光-观看率", "SUM(watch_count) / NULLIF(SUM(exposure_count_est), 0) * 100"),
        ("观看-关注率", "SUM(new_fan_count) / NULLIF(SUM(watch_count), 0) * 100"),
        ("观看-互动率", "SUM(interaction_count) / NULLIF(SUM(watch_count), 0) * 100"),
        ("观看-成交率", "SUM(deal_count) / NULLIF(SUM(watch_count), 0) * 100"),
    ]
    add_table(doc, ["指标", "聚合公式"], rate_rows, [Cm(4.1), Cm(13.0)], font_size=9.0)

    doc.add_page_break()
    add_heading(doc, "五 常用 SQL 示例", 1)
    add_heading(doc, "查询主播区间明细", 2)
    add_code(doc, """SELECT
  business_date, room_name, anchor_name,
  segment_start_at, segment_end_at, duration_seconds,
  watch_count, new_fan_count, deal_count,
  gmv_amount, ad_cost_amount, quality_status, quality_flags
FROM bi_live_anchor_segment_fact
WHERE is_deleted = 0
  AND business_date BETWEEN '2026-11-01' AND '2026-11-30'
ORDER BY business_date, room_id, segment_start_at;""")

    add_heading(doc, "按日期和主播汇总", 2)
    add_code(doc, """SELECT
  business_date,
  room_id,
  room_name,
  anchor_user_id,
  anchor_name,
  ROUND(SUM(duration_seconds) / 3600, 2) AS live_hours,
  SUM(watch_count) AS watch_count,
  SUM(new_fan_count) AS new_fan_count,
  SUM(deal_count) AS deal_count,
  SUM(gmv_amount) AS gmv_amount,
  SUM(ad_cost_amount) AS ad_cost_amount,
  ROUND(SUM(new_fan_count) / NULLIF(SUM(watch_count), 0) * 100, 2)
    AS watch_follow_rate,
  ROUND(SUM(deal_count) / NULLIF(SUM(watch_count), 0) * 100, 2)
    AS watch_deal_rate
FROM bi_live_anchor_segment_fact
WHERE is_deleted = 0
  AND quality_status <> 'INVALID'
GROUP BY business_date, room_id, room_name, anchor_user_id, anchor_name
ORDER BY business_date, room_id, gmv_amount DESC;""")

    add_heading(doc, "按直播间汇总全部转化率", 2)
    add_code(doc, """SELECT
  business_date,
  room_name,
  SUM(watch_count) AS watch_count,
  SUM(interaction_count) AS interaction_count,
  SUM(new_fan_count) AS new_fan_count,
  SUM(deal_count) AS deal_count,
  ROUND(SUM(watch_count) / NULLIF(SUM(exposure_count_est), 0) * 100, 2)
    AS exposure_watch_rate,
  ROUND(SUM(new_fan_count) / NULLIF(SUM(watch_count), 0) * 100, 2)
    AS watch_follow_rate,
  ROUND(SUM(interaction_count) / NULLIF(SUM(watch_count), 0) * 100, 2)
    AS watch_interaction_rate,
  ROUND(SUM(deal_count) / NULLIF(SUM(watch_count), 0) * 100, 2)
    AS watch_deal_rate
FROM bi_live_anchor_segment_fact
WHERE is_deleted = 0
  AND quality_status <> 'INVALID'
GROUP BY business_date, room_id, room_name;""")

    add_heading(doc, "检查需要关注的数据", 2)
    add_code(doc, """SELECT
  segment_id, business_date, room_name, anchor_name,
  segment_start_at, segment_end_at,
  precision_status, quality_status, quality_flags
FROM bi_live_anchor_segment_fact
WHERE is_deleted = 0
  AND quality_status IN ('WARNING', 'INVALID')
ORDER BY business_date DESC, segment_start_at DESC;""")

    add_heading(doc, "六 示例结果", 1)
    example_rows = [
        ("2026-11-03", "一号直播间", "示例主播甲", "3.00", "1,363", "40", "17", "57,311.00", "1,353.09", "VALID"),
        ("2026-11-03", "一号直播间", "示例主播乙", "2.50", "1,120", "31", "12", "42,680.00", "1,110.50", "WARNING"),
    ]
    add_table(
        doc,
        ["日期", "直播间", "主播", "小时", "观看", "涨粉", "成交", "GMV", "消耗", "质量"],
        example_rows,
        [Cm(2.0), Cm(2.2), Cm(2.2), Cm(1.2), Cm(1.4), Cm(1.2), Cm(1.2), Cm(2.1), Cm(1.9), Cm(1.6)],
        font_size=7.8,
    )
    add_body(doc, "示例中的 WARNING 不等于数据不可用。应结合 quality_flags 判断原因，例如 SESSION_STALE 表示场次状态长期未关闭，NON_EXACT 表示区间边界不是精确快照。")

    add_heading(doc, "七 质量标记说明", 1)
    quality_rows = [
        ("INVALID_TIME", "结束时间为空或不晚于开始时间", "排除并修正时间"),
        ("MISSING_ROOM", "缺少直播间", "补齐直播间映射"),
        ("MISSING_ANCHOR", "缺少主播", "补齐主播信息"),
        ("NEGATIVE_METRIC", "区间指标出现负数", "核查累计快照或补录值"),
        ("LONGER_THAN_12H", "单个主播区间超过 12 小时", "确认是否漏报中间交接"),
        ("NON_EXACT", "区间边界不是精确快照", "可统计，需标记精度"),
        ("MANUAL_SOURCE", "来源为手工补录", "可统计，保留人工来源标记"),
        ("MISSING_EXPOSURE_RATE", "有观看人数但缺少曝光-观看率", "曝光量及相关聚合率不可计算"),
        ("SESSION_STALE", "场次长时间保持 LIVE", "检查场次是否应结束"),
    ]
    add_table(doc, ["标记", "含义", "建议处理"], quality_rows, [Cm(4.0), Cm(6.5), Cm(6.6)], font_size=8.6)

    add_heading(doc, "八 数据更新和审计", 1)
    add_bullet(doc, "业务上报、补录、修改或作废提交成功后，系统在事务提交后同步对应 segment_id。")
    add_bullet(doc, "每 5 分钟执行一次兜底增量同步，补偿短暂失败或遗漏的事件。")
    add_bullet(doc, "每日执行源表与事实表对账，核对数量、时长及全部可加指标。")
    add_bullet(doc, "作废记录不会物理删除，事实表通过 is_deleted = 1 保留审计痕迹。")
    add_bullet(doc, "source_version、source_updated_at、synced_at 和 row_hash 可用于排查同步时序和一致性。")

    doc.add_page_break()
    add_heading(doc, "九 BI 接入检查清单", 1)
    checklist = [
        "数据源指向 xinkeerp.bi_live_anchor_segment_fact。",
        "模型默认过滤 is_deleted = 0。",
        "正式指标排除 quality_status = 'INVALID'。",
        "日期维度使用 business_date，不使用 DATE(segment_start_at) 代替。",
        "金额按元展示；duration_seconds 转换成小时后再展示。",
        "四个百分比使用本文公式重算，不使用 AVG。",
        "增量抽取优先使用 source_updated_at + segment_id，展示同步延迟时使用 synced_at。",
        "报表中保留 quality_status 和 quality_flags，便于定位异常。",
    ]
    for item in checklist:
        add_bullet(doc, "□ " + item)

    add_body(doc, "文档版本：1.0    数据表：bi_live_anchor_segment_fact    更新日期：2026-11-03")
    doc.core_properties.title = "主播直播区间事实表使用说明"
    doc.core_properties.subject = "bi_live_anchor_segment_fact 数据交接与 BI 查询指南"
    doc.core_properties.author = "新客数据团队"
    doc.save(OUTPUT)
    print(OUTPUT)


if __name__ == "__main__":
    build()
