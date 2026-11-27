const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../MetricSelection.js'), 'utf8');
const key = 'COMPASS_LIVE_SCEEN_CORE_INDICATORS_QIANCHUAN_TALENT_HIT';
const required = ['follow_anchor_ucnt', 'stat_cost', 'pay_deposit_pre_order_cnt',
        'presale_depay_deamt', 'watch_ucnt', 'pay_ucnt'];

function run(initial, options = {}) {
    const storage = new Map([[key, initial], ['unrelated-preference', 'preserve']]);
    const messages = [];
    let writes = 0;
    const window = { chrome: { webview: { postMessage: value => messages.push(value) } } };
    window.top = options.frame ? {} : window;
    const context = vm.createContext({
        window,
        location: { origin: options.origin ?? 'https://compass.jinritemai.com',
            pathname: options.pathname ?? '/screen/live/talent' },
        localStorage: {
            getItem: name => { if (options.blocked) throw new Error('blocked'); return storage.get(name) ?? null; },
            setItem: (name, value) => {
                if (options.writeBlocked) throw new Error('blocked');
                writes++; storage.set(name, value);
            }
        }
    });
    const result = vm.runInContext(source, context);
    assert.equal(storage.get('unrelated-preference'), 'preserve');
    return { result, storage, messages, writes };
}

for (const initial of [null, '{malformed', '{}', JSON.stringify(required.slice(0, 5)),
    JSON.stringify([...required.slice(0, 5), required[0]]), JSON.stringify([...required, 'current_cnt'])]) {
    const test = run(initial);
    assert.equal(test.result.result, 'updated');
    assert.deepEqual(JSON.parse(test.storage.get(key)), required);
    assert.equal(test.writes, 1);
    assert.equal(test.messages[0].result, 'updated');
}
const ready = run(JSON.stringify([...required].reverse()));
assert.equal(ready.result.result, 'ready');
assert.equal(ready.writes, 0);
assert.equal(run(null, { blocked: true }).result.result, 'blocked');
assert.equal(run(null, { writeBlocked: true }).result.result, 'blocked');
for (const options of [{ frame: true }, { origin: 'https://buyin.jinritemai.com' },
    { origin: 'http://compass.jinritemai.com' }, { pathname: '/login/transfer' }]) {
    const skipped = run(null, options);
    assert.equal(skipped.result.result, 'skipped');
    assert.equal(skipped.writes, 0);
    assert.equal(skipped.messages.length, 0);
}
console.log('Metric selection: incomplete/malformed repair, idempotency, storage failure and page/frame guards passed.');
