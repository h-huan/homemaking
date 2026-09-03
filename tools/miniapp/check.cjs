// Node-only contract checks; WeChat Developer Tools/device acceptance is still required.
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const assert = require('node:assert/strict')
const root = path.resolve(__dirname, '../../hm-miniapp')
function walk(dir) { return fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]) }
function pending() { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }
function page(file, api = {}, wx = {}) {
  let options
  vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), {
    require: name => name.endsWith('/page') ? value => { options = value } : api,
    wx, setTimeout: fn => fn(), getApp: () => ({ globalData: {} })
  }, { filename: file })
  options.data = JSON.parse(JSON.stringify(options.data))
  options.setData = function (values) {
    for (const [key, value] of Object.entries(values)) {
      const fields = key.split('.'); let target = this.data
      for (const field of fields.slice(0, -1)) target = target[field] || (target[field] = {})
      target[fields.at(-1)] = value
    }
  }
  return options
}
async function run() {
  const scripts = walk(root).filter(f => f.endsWith('.js'))
  for (const file of scripts) new vm.Script(fs.readFileSync(file, 'utf8'), { filename: file })
  const config = JSON.parse(fs.readFileSync(path.join(root, 'app.json'), 'utf8'))
  for (const route of config.pages) for (const ext of ['js', 'wxml', 'json', 'wxss']) assert.ok(fs.existsSync(path.join(root, `${route}.${ext}`)), `${route}.${ext} missing`)
  console.log(`PASS: ${scripts.length} JavaScript files parse; ${config.pages.length} page routes have all files.`)
  const first = pending(), second = pending(); let queries = 0
  const booking = page('pages/order/booking/index.js', { getCapacity: () => (++queries === 1 ? first.promise : second.promise) })
  booking.data.form = { serviceItemId: 1, addressId: 1, appointmentDate: '2099-01-01' }
  const old = booking.loadCapacity(); booking.data.form.appointmentDate = '2099-01-02'; const latest = booking.loadCapacity()
  second.resolve([{ available: true, label: '13:00-15:00' }]); await latest
  first.resolve([{ available: true, label: '09:00-11:00' }]); await old
  assert.equal(booking.data.form.appointmentTimeSlot, '13:00-15:00'); assert.equal(booking.data.capacityLoading, false)
  const oldPrice = pending(), newPrice = pending(); let prices = 0
  const priced = page('pages/order/booking/index.js', { calcOrder: () => (++prices === 1 ? oldPrice.promise : newPrice.promise) })
  priced.data.form.addressId = 1
  const a = priced.calcPrice(); priced.data.selectedExtraIds = [2]; const b = priced.calcPrice()
  newPrice.resolve({ totalCents: 20000 }); await b; oldPrice.resolve({ totalCents: 10000 }); await a
  assert.equal(priced.data.calc.totalCents, 20000); assert.equal(priced.data.form.extraItemList[0].extraItemId, 2)
  const submitted = pending(); let submissions = 0
  const confirm = page('pages/order/confirm/index.js', { submitOrder: () => { submissions++; return submitted.promise } }, { removeStorageSync() {}, redirectTo() {} })
  confirm.data.booking = { form: {} }; const one = confirm.submitOrder(); await confirm.submitOrder()
  assert.equal(submissions, 1); submitted.resolve({ orderId: 1 }); await one
  const moveFirst = pending(), moveSecond = pending(); let moves = 0
  const reschedule = page('pages/order/reschedule/index.js', { getOrderCapacity: () => (++moves === 1 ? moveFirst.promise : moveSecond.promise) })
  reschedule.orderId = 1; reschedule.data.date = '2099-01-01'; const c = reschedule.loadCapacity()
  reschedule.data.date = '2099-01-02'; const d = reschedule.loadCapacity()
  moveSecond.resolve([{ available: true, label: '16:00-18:00' }]); await d
  moveFirst.resolve([{ available: true, label: '09:00-11:00' }]); await c
  assert.equal(reschedule.data.slot, '16:00-18:00')
  console.log('PASS: booking and reschedule ignore stale capacity; latest quotation wins; double submission blocked.')
  let prepared = 0, requested = 0, synced = 0
  const detail = page('pages/order/detail/index.js', {
    preparePayment: async () => { prepared++; return { status: 0, displayContent: JSON.stringify({ timeStamp: 'test-time', nonceStr: 'test-nonce', packageValue: 'test-package', signType: 'RSA', paySign: 'TEST_ONLY' }) } },
    syncPayment: async () => { synced++ }
  }, { requestPayment: options => { requested++; options.success() }, showToast() {} })
  detail.data.detail = { orderId: 1, orderStatus: '10', paymentOptions: { onlineAvailable: false, offlineAvailable: true } }
  detail.loadDetail = async () => { detail.data.detail.orderStatus = '30' }
  await detail.handlePay(); assert.equal(prepared, 0); assert.equal(requested, 0)
  detail.data.detail.paymentOptions.onlineAvailable = true
  await detail.handlePay(); assert.equal(prepared, 1); assert.equal(requested, 1); assert.equal(synced, 1)
  const template = fs.readFileSync(path.join(root, 'pages/order/detail/index.wxml'), 'utf8')
  assert.match(template, /wx:if="\{\{detail.orderStatus === '10' && detail.paymentOptions.onlineAvailable\}\}"[^>]+bindtap="handlePay"/)
  console.log('PASS: unavailable online payment is hidden and cannot be invoked; enabled WeChat payment still submits and reconciles.')
}
run().catch(error => { console.error(error); process.exitCode = 1 })
