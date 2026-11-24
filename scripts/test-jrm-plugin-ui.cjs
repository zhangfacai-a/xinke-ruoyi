const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const { preview, missingBlueCardMetrics } = require('../clients/extensions/jinritemai-shift-report/ui-state.js')
require('../clients/extensions/jinritemai-shift-report/dom-collector.js')

const current = { capturedAt: 2000, watchTotal: 200, interactionTotal: 20, newFanTotal: 8, dealTotal: 4, gmvTotal: 2500, adCostTotal: 300, presaleOrderTotal: 1 }
const first = preview(current, null, '2026-10-30 06:00:00')
assert.equal(first.firstReport, true)
assert.equal(first.startTime, '2026-10-30 06:00:00')
assert.equal(first.delta.watchTotal, 200)

const previous = { snapshotId: 9, capturedAt: '2026-10-30 08:00:00', watchTotal: 150, interactionTotal: 12, newFanTotal: 5, dealTotal: 2, gmvTotal: 1800, adCostTotal: 210, presaleOrderTotal: 0 }
const next = preview(current, previous, '2026-10-30 06:00:00')
assert.equal(next.firstReport, false)
assert.equal(next.previousSnapshotId, 9)
assert.equal(next.delta.watchTotal, 50)
assert.equal(next.delta.gmvTotal, 700)
assert.equal(next.intervalRate.watchFollowRate, 6)
const ratePreview = preview(
  { ...current, exposureWatchRate: 3, watchFollowRate: 0.56, watchInteractionRate: 1.12, watchDealRate: 0.12 },
  { ...previous, exposureWatchRate: 3.2, watchFollowRate: 0.5, watchInteractionRate: 1.2, watchDealRate: 0.1 },
  '2026-10-30 06:00:00'
)
assert.equal(ratePreview.intervalRate.exposureWatchRate, 2.53)
assert.equal(ratePreview.intervalRate.watchFollowRate, 6)
const corrected = preview({ ...current, watchTotal: 100, gmvTotal: 1600 }, previous, '')
assert.equal(corrected.delta.watchTotal, -50)
assert.equal(corrected.delta.gmvTotal, -200)
assert.deepEqual(corrected.adjustments, ['watchTotal', 'gmvTotal'])

const completeMetrics = Object.fromEntries([
  'gmv', 'watch_ucnt', 'follow_anchor_ucnt', 'pay_ucnt', 'stat_cost', 'live_show_watch_cnt_ratio',
  'watch_follow_ucnt_ratio', 'watch_interact_ucnt_ratio', 'watch_pay_ucnt_ratio', 'pay_deposit_pre_order_cnt'
].map(key => [key, { value: '0' }]))
assert.deepEqual(missingBlueCardMetrics(completeMetrics), [])
delete completeMetrics.follow_anchor_ucnt
delete completeMetrics.watch_interact_ucnt_ratio
assert.deepEqual(missingBlueCardMetrics(completeMetrics), ['新增粉丝数', '观看-互动率(人数)'])

const content = fs.readFileSync(path.join(__dirname, '../clients/extensions/jinritemai-shift-report/content.js'), 'utf8')
const collector = fs.readFileSync(path.join(__dirname, '../clients/extensions/jinritemai-shift-report/dom-collector.js'), 'utf8')
assert.match(content, /继续上报/)
assert.match(content, /state\.context = \{ \.\.\.result, roomMatched: true, roomId/)
assert.match(content, /clearPreview\(\); renderSuccess\(value\)/)
assert.match(content, /\$\('anchor'\)\.value = ''/)
assert.match(content, /#f26b21/)
assert.match(content, /id="connection">连接中/)
assert.match(content, /读取结果已超过 2 分钟/)
assert.match(content, /connection\('', '已连接'\)/)
assert.match(content, /connection\('error', '连接异常'\)/)
assert.match(content, /平台校正值保留负差额/)
assert.match(content, /missingBlueCardMetrics/)
assert.match(content, /resultMetric\.textContent = changeText/)
assert.match(content, /曝光-观看率\(次数\)/)
assert.match(content, /本次数据确认/)
assert.match(content, /确认并上报/)
assert.match(content, /直播累计/)
assert.doesNotMatch(content, /本时段/)
assert.match(content, /grid-template-columns:repeat\(3,minmax\(0,1fr\)\)/)
assert.match(content, /head\.addEventListener\('pointerdown'/)
assert.match(collector, /findBlueCardRoot/)
assert.match(collector, /readMetric\(label, percent, root\)/)

const odometerPart = (className, values = [], current = null, value = '') => ({
  classList: { contains: name => className.split(' ').includes(name) },
  textContent: value,
  querySelectorAll: selector => selector === '.odometer-value' ? values.map(textContent => ({ textContent })) : [],
  querySelector: selector => selector === '.odometer-last-value' && current != null ? { textContent: current } : null
})
const odometer = { children: [
  odometerPart('odometer-digit', ['7', '4'], '4'),
  odometerPart('odometer-formatting-mark', [], null, ','),
  odometerPart('odometer-digit', ['3', '5'], '5'),
  odometerPart('odometer-digit', ['1', '9'], '9'),
  odometerPart('odometer-digit', ['0', '7'], '7')
] }
assert.equal(globalThis.XinkeJrmDomCollector.readOdometer(odometer), '4,597')
console.log('PASS single-report preview')
