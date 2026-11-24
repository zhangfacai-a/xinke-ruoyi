// Verified in Chrome on 2026-11-24: the talent screen reads this local preference
// on load and uses it to build/sign its own core_data request.
(() => {
    if (window !== window.top || location.origin !== 'https://compass.jinritemai.com' ||
        location.pathname !== '/screen/live/talent') return { result: 'skipped' };

    const required = ['watch_ucnt', 'pay_ucnt', 'stat_cost', 'watch_pay_ucnt_ratio',
        'pay_deposit_pre_order_cnt', 'live_show_watch_cnt_ratio', 'watch_interact_ucnt_ratio',
        'watch_follow_ucnt_ratio', 'follow_anchor_ucnt'];
    const key = 'COMPASS_LIVE_SCEEN_CORE_INDICATORS_QIANCHUAN_TALENT_HIT';
    const report = { type: 'xinke-metric-selection', result: 'blocked' };
    try {
        const raw = localStorage.getItem(key);
        let previous;
        try { previous = JSON.parse(raw); } catch { previous = null; }
        const complete = Array.isArray(previous) && previous.length === required.length &&
            required.every(name => previous.includes(name));
        if (!complete) localStorage.setItem(key, JSON.stringify(required));
        report.result = complete ? 'ready' : 'updated';
    } catch { /* Storage failure is reported; missing values must not become zero. */ }
    try { window.chrome?.webview?.postMessage(report); } catch { }
    return report;
})();
