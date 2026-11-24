(function (global) {
  'use strict'

  const numeric = value => Number(value || 0)
  const decimal = (value, digits = 2) => Number(numeric(value).toFixed(digits))
  const requiredMetrics = [
    ['gmv', '直播间成交金额'], ['watch_ucnt', '累计观看人数'], ['follow_anchor_ucnt', '新增粉丝数'],
    ['pay_ucnt', '成交人数'], ['stat_cost', '千川消耗'], ['live_show_watch_cnt_ratio', '曝光-观看率(次数)'],
    ['watch_follow_ucnt_ratio', '观看-关注率(人数)'], ['watch_interact_ucnt_ratio', '观看-互动率(人数)'],
    ['watch_pay_ucnt_ratio', '观看-成交率(人数)'], ['pay_deposit_pre_order_cnt', '预售订单数']
  ]

  function missingBlueCardMetrics (metrics = {}) {
    return requiredMetrics
      .filter(([key]) => !metrics[key] || metrics[key].value == null || String(metrics[key].value).trim() === '')
      .map(([, label]) => label)
  }

  function preview (current, previous, liveStartTime) {
    const baseline = previous || {}
    const adjustments = []
    const delta = (key, digits) => {
      const value = numeric(current[key]) - numeric(baseline[key])
      const result = digits == null ? Math.round(value) : decimal(value, digits)
      if (result < 0) adjustments.push(key)
      return result
    }
    const intervalRate = (numerator, denominator) => denominator > 0 && numerator >= 0 ? decimal(numerator * 100 / denominator, 2) : null
    const exposureTotal = snapshot => {
      const watch = numeric(snapshot?.watchTotal)
      const rate = numeric(snapshot?.exposureWatchRate)
      if (watch === 0) return 0
      return rate > 0 ? watch * 100 / rate : null
    }
    const watchDelta = delta('watchTotal')
    const interactionDelta = delta('interactionTotal')
    const fanDelta = delta('newFanTotal')
    const dealDelta = delta('dealTotal')
    const currentExposure = exposureTotal(current)
    const previousExposure = previous ? exposureTotal(previous) : 0
    const exposureDelta = currentExposure == null || previousExposure == null ? null : currentExposure - previousExposure
    return {
      firstReport: !previous,
      previousSnapshotId: previous?.snapshotId || null,
      startTime: previous?.capturedAt || liveStartTime,
      endTime: current.capturedAt,
      current,
      previous: previous || null,
      adjustments,
      intervalRate: {
        exposureWatchRate: exposureDelta == null ? null : intervalRate(watchDelta, exposureDelta),
        watchFollowRate: intervalRate(fanDelta, watchDelta),
        watchInteractionRate: intervalRate(interactionDelta, watchDelta),
        watchDealRate: intervalRate(dealDelta, watchDelta)
      },
      delta: {
        watchTotal: watchDelta, interactionTotal: interactionDelta, newFanTotal: fanDelta,
        dealTotal: dealDelta, gmvTotal: delta('gmvTotal', 2), adCostTotal: delta('adCostTotal', 2),
        presaleOrderTotal: delta('presaleOrderTotal')
      }
    }
  }

  const api = Object.freeze({ preview, missingBlueCardMetrics })
  global.XinkeJrmUiState = api
  if (typeof module !== 'undefined' && module.exports) module.exports = api
})(globalThis)
