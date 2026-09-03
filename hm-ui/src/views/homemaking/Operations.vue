<template>
  <HmPage
    eyebrow="HM · 日常运营"
    title="把每一次服务，安排妥当。"
    description="维护门店、人员与服务，处理订单、评价和售后。"
  >
    <template #actions
      ><el-button
        v-if="isCatalog && can(`${tab}:write`) && (tab !== 'stores' || range === 'TENANT')"
        type="primary"
        @click="edit()"
        >新增{{ tabLabel }}</el-button
      ></template
    >
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane v-for="item in tabs" :key="item.key" :label="item.label" :name="item.key" />
    </el-tabs>
    <div class="hm-toolbar"
      ><span>{{ total }} 条记录</span
      ><el-button :loading="loading" @click="load">刷新</el-button></div
    >
    <el-table v-loading="loading" :data="rows" stripe empty-text="暂时没有记录">
      <el-table-column prop="id" label="编号" width="85" />
      <template v-if="isCatalog">
        <el-table-column prop="name" :label="tabLabel + '名称'" min-width="180" />
        <el-table-column v-if="tab === 'stores'" prop="address" label="地址" min-width="240" />
        <el-table-column v-if="tab === 'workers'" prop="skills" label="擅长服务" min-width="180" />
        <el-table-column v-if="tab !== 'stores'" prop="store_id" label="门店编号" width="110" />
        <el-table-column v-if="tab === 'services'" label="价格" width="120"
          ><template #default="{ row }">¥{{ money(row.price_cents) }}</template></el-table-column
        >
        <el-table-column
          v-if="tab === 'services'"
          prop="duration_minutes"
          label="时长（分钟）"
          width="130"
        />
      </template>
      <template v-else-if="tab === 'orders'">
        <el-table-column prop="service_name" label="服务" min-width="180" /><el-table-column
          prop="contact_name"
          label="客户"
          width="110"
        />
        <el-table-column label="订单金额" width="120"
          ><template #default="{ row }">¥{{ money(row.price_cents) }}</template></el-table-column
        >
        <el-table-column prop="worker_id" label="服务人员编号" width="130" />
      </template>
      <template v-else-if="tab === 'customers'"
        ><el-table-column prop="nickname" label="客户昵称" min-width="180" /><el-table-column
          prop="source"
          label="来源"
          width="150"
      /></template>
      <template v-else-if="tab === 'aftersales'"
        ><el-table-column prop="order_id" label="订单编号" width="120" /><el-table-column
          prop="reason"
          label="申请原因"
          min-width="240"
        /><el-table-column label="申请退款"
          ><template #default="{ row }">¥{{ money(row.amount_cents) }}</template></el-table-column
        ></template
      >
      <template v-else-if="tab === 'settlements'"
        ><el-table-column prop="order_id" label="订单编号" /><el-table-column label="服务收入"
          ><template #default="{ row }">¥{{ money(row.gross_cents) }}</template></el-table-column
        ><el-table-column label="退款"
          ><template #default="{ row }">¥{{ money(row.refund_cents) }}</template></el-table-column
        ><el-table-column label="待结算"
          ><template #default="{ row }">¥{{ money(row.net_cents) }}</template></el-table-column
        ></template
      >
      <template v-if="tab === 'reviews'"
        ><el-table-column prop="order_id" label="订单" width="100" /><el-table-column
          prop="rating"
          label="评分"
          width="80" /><el-table-column
          prop="content"
          label="评价内容"
          min-width="300" /><el-table-column label="官网展示" width="120"
          ><template #default="{ row }"
            ><el-switch
              :model-value="!!row.visible"
              :disabled="!can('reviews:moderate')"
              @change="reviewVisibility(row, !!$event)" /></template></el-table-column
      ></template>
      <el-table-column v-if="tab !== 'reviews'" label="状态" width="120"
        ><template #default="{ row }"
          ><el-tag
            :type="row.status === 'ACTIVE' || row.status === 'COMPLETED' ? 'success' : 'info'"
            >{{ statusLabel(row.status) }}</el-tag
          ></template
        ></el-table-column
      >
      <el-table-column
        v-if="isCatalog || tab === 'orders' || tab === 'aftersales'"
        label="操作"
        min-width="230"
        fixed="right"
        ><template #default="{ row }">
          <el-button v-if="isCatalog && can(`${tab}:write`)" link type="primary" @click="edit(row)"
            >编辑</el-button
          >
          <el-button
            v-if="tab === 'services' && can('services:write')"
            link
            type="primary"
            @click="openSettings(row)"
            >价格与预约规则</el-button
          >
          <template v-if="tab === 'orders'">
            <el-button link type="primary" @click="showOrder(row.id)">详情</el-button>
            <el-button
              v-if="
                can('orders:change') &&
                !row.pending_change_id &&
                ['UNPAID', 'PAID', 'ASSIGNED'].includes(row.status) &&
                ['WAITING', 'ACCEPTED'].includes(row.fulfillment_status)
              "
              link
              type="primary"
              @click="openChange(row)"
              >变更地址/价格</el-button
            >
            <el-button
              v-if="
                (row.status === 'UNPAID' || row.change_status === 'PENDING_PAYMENT') &&
                !row.pay_order_id &&
                can('payment:receive')
              "
              link
              type="primary"
              @click="openPayment('RECEIPT', row)"
              >{{ row.change_status === 'PENDING_PAYMENT' ? '确认补款' : '确认收款' }}</el-button
            >
            <el-button
              v-if="
                ['UNPAID', 'PAID', 'ASSIGNED'].includes(row.status) &&
                ['WAITING', 'ACCEPTED'].includes(row.fulfillment_status)
              "
              link
              :disabled="!!row.pending_change_id || !can('orders:reschedule')"
              @click="openReschedule(row)"
              >改期</el-button
            >
            <el-button
              v-if="['PAID', 'ASSIGNED'].includes(row.status)"
              link
              type="primary"
              :disabled="!!row.pending_change_id || !can('orders:dispatch')"
              @click="beginAssign(row)"
              >派单</el-button
            >
            <el-button
              v-if="row.status === 'ASSIGNED' && row.fulfillment_status === 'ARRIVED'"
              link
              type="primary"
              :disabled="!can('orders:fulfill')"
              @click="action(row, 'start')"
              >开始服务</el-button
            >
            <el-button
              v-if="row.status === 'IN_SERVICE' && row.fulfillment_status === 'STARTED'"
              link
              type="primary"
              :disabled="!can('orders:fulfill')"
              @click="action(row, 'complete')"
              >确认完工</el-button
            >
            <el-button
              v-if="['UNPAID', 'PAID', 'ASSIGNED'].includes(row.status)"
              link
              type="danger"
              :disabled="!can('orders:cancel')"
              @click="action(row, 'cancel')"
              >取消订单</el-button
            >
          </template>
          <template v-if="tab === 'aftersales'">
            <el-button
              v-if="
                row.status === 'REQUESTED' &&
                row.payment_method === 'OFFLINE' &&
                can('aftersales:refund')
              "
              link
              type="primary"
              @click="openPayment('REFUND', row)"
              >登记线下退款</el-button
            >
            <el-button
              v-if="row.status === 'REQUESTED' && !row.order_change_id"
              link
              :disabled="!can('aftersales:reject')"
              @click="reject(row)"
              >驳回</el-button
            ><el-button
              v-if="row.status === 'REQUESTED' && row.payment_method !== 'OFFLINE'"
              link
              type="danger"
              :disabled="!can('aftersales:refund')"
              @click="refund(row, 'approve')"
              >批准退款</el-button
            ><el-button
              v-if="row.status === 'REFUNDING'"
              link
              :disabled="!can('aftersales:refund')"
              @click="refund(row, 'sync')"
              >查询退款结果</el-button
            ></template
          >
        </template></el-table-column
      >
    </el-table>
    <el-pagination
      v-if="isCatalog || tab === 'orders'"
      class="hm-pagination"
      v-model:current-page="page"
      :page-size="20"
      :total="total"
      layout="prev, pager, next"
      @current-change="load"
    />
    <el-dialog v-model="editing" :title="(form.id ? '编辑' : '新增') + tabLabel" width="580px">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="名称" prop="name" :rules="[{ required: true, message: '请填写名称' }]"
          ><el-input v-model="form.name" maxlength="100"
        /></el-form-item>
        <el-form-item
          v-if="tab !== 'stores'"
          label="所属门店"
          prop="storeId"
          :rules="[{ required: true, message: '请选择门店' }]"
          ><el-select v-model="form.storeId" class="w-full"
            ><el-option
              v-for="store in stores"
              :key="store.id"
              :label="store.name"
              :value="store.id" /></el-select
        ></el-form-item>
        <template v-if="tab === 'stores'"
          ><el-form-item label="门店地址"
            ><el-input v-model="form.address" maxlength="500" /></el-form-item
          ><el-form-item label="服务区域"
            ><el-input v-model="form.serviceArea" maxlength="500" /></el-form-item
        ></template>
        <el-form-item v-if="tab !== 'services'" label="联系电话"
          ><el-input v-model="form.phone" maxlength="32"
        /></el-form-item>
        <el-form-item v-if="tab === 'workers'" label="擅长服务"
          ><el-input v-model="form.skills" maxlength="1000"
        /></el-form-item>
        <el-form-item v-if="tab === 'workers'" label="人员头像地址"
          ><el-input v-model="form.avatar" placeholder="HTTPS 图片地址"
        /></el-form-item>
        <el-form-item v-if="tab === 'services'" label="服务封面地址"
          ><el-input v-model="form.cover" placeholder="HTTPS 图片地址"
        /></el-form-item>
        <template v-if="tab === 'services'"
          ><el-form-item label="分类"
            ><el-input v-model="form.category" maxlength="100" /></el-form-item
          ><el-form-item label="服务说明"
            ><el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              maxlength="10000" /></el-form-item
          ><div class="hm-form-row"
            ><el-form-item label="价格（元）"
              ><el-input-number v-model="form.priceYuan" :min="0.01" :precision="2" /></el-form-item
            ><el-form-item label="时长（分钟）"
              ><el-input-number
                v-model="form.durationMinutes"
                :min="30"
                :max="480"
                :step="30"
                step-strictly /></el-form-item></div
        ></template>
        <el-form-item label="状态"
          ><el-radio-group v-model="form.status"
            ><el-radio value="ACTIVE">启用</el-radio
            ><el-radio value="INACTIVE">停用</el-radio></el-radio-group
          ></el-form-item
        > </el-form
      ><template #footer
        ><el-button @click="editing = false">取消</el-button
        ><el-button :loading="saving" type="primary" @click="save">保存</el-button></template
      >
    </el-dialog>
    <el-dialog v-model="assigning" title="安排服务人员" width="420px"
      ><el-select v-model="workerId" class="w-full" placeholder="请选择服务人员"
        ><el-option
          v-for="worker in workers"
          :key="worker.id"
          :label="worker.name"
          :value="worker.id" /></el-select
      ><template #footer
        ><el-button type="primary" :disabled="!workerId" @click="assign"
          >确认派单</el-button
        ></template
      ></el-dialog
    >
    <el-dialog v-model="rescheduling" title="调整预约时间" width="min(460px,94vw)"
      ><el-form label-position="top"
        ><el-form-item label="新预约时间"
          ><el-date-picker
            v-model="newStart"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item
        ><el-form-item label="调整原因"
          ><el-input v-model="rescheduleReason" maxlength="500" /></el-form-item></el-form
      ><template #footer
        ><el-button type="primary" @click="reschedule">确认改期</el-button></template
      ></el-dialog
    >
    <el-drawer v-model="detailVisible" title="订单详情与履约记录" size="min(760px,96vw)"
      ><template v-if="detail"
        ><el-descriptions :column="1" border
          ><el-descriptions-item label="服务">{{ detail.service_name }}</el-descriptions-item
          ><el-descriptions-item label="时间"
            >{{ formatDate(detail.booking?.starts_at) }} —
            {{ formatDate(detail.booking?.ends_at) }}</el-descriptions-item
          ><el-descriptions-item label="支付方式">{{
            paymentMethods[detail.payment_method]
          }}</el-descriptions-item>
          <el-descriptions-item label="履约状态">{{
            labels[detail.fulfillment_status] || detail.fulfillment_status
          }}</el-descriptions-item>
          <el-descriptions-item
            v-if="detail.fulfillment_status === 'AWAITING_CONFIRMATION'"
            label="客户确认期限"
            >{{ formatDate(detail.confirmation_deadline) }}；存在进行中售后时暂停自动确认</el-descriptions-item
          >
          <el-descriptions-item label="应收 / 已收 / 已退"
            >￥{{ money(detail.price_cents) }} / ￥{{ money(detail.paid_cents) }} / ￥{{
              money(detail.refunded_cents)
            }}</el-descriptions-item
          ><el-descriptions-item label="地址">{{ detail.address }}</el-descriptions-item
          ><el-descriptions-item label="联系人"
            >{{ detail.contact_name }} · {{ detail.phone }}</el-descriptions-item
          ></el-descriptions
        ><el-table v-if="can('payment:read')" :data="paymentEntries" empty-text="暂无收支凭证">
          <el-table-column prop="id" label="流水" width="80" /><el-table-column
            label="类型"
            width="100"
            ><template #default="{ row }">{{ paymentKinds[row.kind] }}</template></el-table-column
          >
          <el-table-column label="渠道" width="120"
            ><template #default="{ row }">{{
              paymentChannels[row.channel]
            }}</template></el-table-column
          >
          <el-table-column label="金额" min-width="110"
            ><template #default="{ row }"
              >￥{{ money(row.amount_cents) }}</template
            ></el-table-column
          >
          <el-table-column label="发生时间" min-width="170">
            <template #default="{ row }">{{ formatDate(row.occurred_at) }}</template>
          </el-table-column>
          <el-table-column label="操作人" min-width="130"
            ><template #default="{ row }">{{
              row.operator_name || (row.operator_id ? '#' + row.operator_id : '支付渠道')
            }}</template></el-table-column
          >
          <el-table-column prop="note" label="备注" min-width="160" />
          <el-table-column v-if="can('payment:reverse')" label="操作" width="100"
            ><template #default="{ row }"
              ><el-button
                v-if="canReverse(row)"
                link
                type="danger"
                @click="openPayment('REVERSAL', row)"
                >冲正</el-button
              ></template
            ></el-table-column
          >
        </el-table>
        <OrderChangeHistory
          :order="detail"
          :can-cancel="
            can('orders:change') &&
            (!detail.pending_change?.difference_cents || can('orders:price'))
          "
          @cancel="cancelChange"
        />
        <el-timeline class="detail-timeline"
          ><el-timeline-item
            v-for="log in detail.logs"
            :key="log.id"
            :timestamp="formatDate(log.created_at)"
            >{{ log.action }} · {{ log.detail }}</el-timeline-item
          ></el-timeline
        ><div class="evidence-grid"
          ><figure v-for="photo in detailPhotos" :key="photo.id"
            ><el-image
              :src="photo.url"
              :preview-src-list="detailPhotos.map((p) => p.url)"
            /><figcaption
              >{{ photo.phase === 'BEFORE' ? '服务前' : '服务后' }} · {{ photo.note }}</figcaption
            ></figure
          ></div
        ></template
      ></el-drawer
    >
    <OrderChangeDialog
      v-model="changeVisible"
      :order="changeTarget"
      :can-price="can('orders:price')"
      @saved="paymentSaved"
    />
    <PaymentEntryDialog
      v-model="paymentVisible"
      :kind="paymentKind"
      :target="paymentTarget"
      @saved="paymentSaved"
    />
    <ServiceSettings v-model="settingsVisible" :service="settingsService" @saved="load" />
  </HmPage>
</template>
<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import * as api from '@/api/homemaking'
import { formatDate } from '@/utils/formatTime'
import HmPage from './components/HmPage.vue'
import { useHmAccess } from './useAccess'
const { can, range, loadAccess } = useHmAccess()
import ServiceSettings from './components/ServiceSettings.vue'
import PaymentEntryDialog from './components/PaymentEntryDialog.vue'
import OrderChangeDialog from './components/OrderChangeDialog.vue'
import OrderChangeHistory from './components/OrderChangeHistory.vue'
import { paymentChannels, paymentKinds, paymentMethods } from './paymentLabels'
const settingsVisible = ref(false),
  settingsService = ref<api.BusinessRow>()
defineOptions({ name: 'HomemakingOperations' })
function openSettings(row: api.BusinessRow) {
  settingsService.value = row
  settingsVisible.value = true
}
const allTabs = [
  { key: 'stores', label: '门店' },
  { key: 'workers', label: '服务人员' },
  { key: 'services', label: '服务' },
  { key: 'orders', label: '订单' },
  { key: 'customers', label: '客户' },
  { key: 'aftersales', label: '售后' },
  { key: 'reviews', label: '评价' },
  { key: 'settlements', label: '结算' }
]
const tabs = computed(() =>
  allTabs.filter((t) => can(`${t.key === 'settlements' ? 'finance' : t.key}:read`))
)
const tab = ref('orders'),
  page = ref(1),
  total = ref(0),
  loading = ref(false),
  saving = ref(false),
  editing = ref(false),
  assigning = ref(false)
const rows = ref<api.BusinessRow[]>([]),
  stores = ref<api.BusinessRow[]>([]),
  workers = ref<api.BusinessRow[]>([]),
  form = ref<api.BusinessRow>({}),
  selectedOrder = ref<api.BusinessRow>({}),
  workerId = ref<number>(),
  formRef = ref<FormInstance>()
const isCatalog = computed(() => ['stores', 'workers', 'services'].includes(tab.value))
const tabLabel = computed(() => tabs.value.find((t) => t.key === tab.value)?.label || '')
const money = (cents: number) => ((cents || 0) / 100).toFixed(2)
const labels: Record<string, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用',
  UNPAID: '待付款',
  PAID: '待派单',
  ASSIGNED: '待服务',
  IN_SERVICE: '服务中',
  AWAITING_CONFIRMATION: '待客户确认完工',
  COMPLETED: '已完工',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  REQUESTED: '待审核',
  PENDING: '待结算',
  REJECTED: '已驳回',
  FAILED: '退款失败'
}
const changeVisible = ref(false),
  changeTarget = ref<api.BusinessRow>()
async function openChange(row: api.BusinessRow) {
  changeTarget.value = await api.getOrderDetail(row.id)
  changeVisible.value = true
}
async function cancelChange(changeId: number) {
  const { value } = await ElMessageBox.prompt(
    '请填写撤销原因，未生效的新地址和价格将作废',
    '撤销订单变更',
    { inputValidator: (v) => !!v?.trim() && v.trim().length <= 500 }
  )
  await api.cancelOrderChange(detail.value!.id, changeId, value.trim())
  await paymentSaved()
}
const paymentVisible = ref(false),
  paymentKind = ref<'RECEIPT' | 'REFUND' | 'REVERSAL'>('RECEIPT'),
  paymentTarget = ref<api.BusinessRow>(),
  paymentEntries = ref<api.BusinessRow[]>([])
async function openPayment(kind: 'RECEIPT' | 'REFUND' | 'REVERSAL', row: api.BusinessRow) {
  if (kind === 'RECEIPT') {
    const current = await api.getOrderDetail(row.id)
    if (!current.payment_options.offlineAvailable)
      return ElMessage.warning(current.payment_options.message)
    row = {
      ...current,
      receipt_amount_cents:
        current.pending_change?.status === 'PENDING_PAYMENT'
          ? current.pending_change.difference_cents
          : current.price_cents
    }
  }
  paymentKind.value = kind
  paymentTarget.value = row
  paymentVisible.value = true
}
function canReverse(row: api.BusinessRow) {
  return (
    row.kind === 'RECEIPT' &&
    !detail.value?.pending_change_id &&
    Number(row.amount_cents) === Number(detail.value?.paid_cents) &&
    row.payment_method === 'OFFLINE' &&
    ['PAID', 'ASSIGNED'].includes(detail.value?.status) &&
    ['WAITING', 'ACCEPTED'].includes(detail.value?.fulfillment_status) &&
    !Number(detail.value?.refunded_cents) &&
    !paymentEntries.value.some((e) => e.reversal_of === row.id)
  )
}
async function paymentSaved() {
  await load()
  if (detailVisible.value && detail.value) await showOrder(detail.value.id)
}
const detailVisible = ref(false),
  detail = ref<any>(),
  detailPhotos = ref<any[]>([]),
  rescheduling = ref(false),
  newStart = ref(''),
  rescheduleReason = ref('')
async function reviewVisibility(row: any, visible: boolean) {
  await api.setReviewVisible(row.id, visible)
  row.visible = visible
  ElMessage.success(visible ? '评价已允许官网展示' : '评价已隐藏')
}
async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('填写驳回原因，客户可在售后进度中查看', '驳回申请', {
    inputValidator: (v) => !!v?.trim() || '请填写原因'
  })
  await api.rejectRefund(row.id, value.trim())
  await load()
}
function openReschedule(row: any) {
  selectedOrder.value = row
  newStart.value = ''
  rescheduleReason.value = ''
  rescheduling.value = true
}
async function reschedule() {
  if (!newStart.value || !rescheduleReason.value.trim())
    return ElMessage.warning('请选择时间并填写原因')
  await api.orderAction(selectedOrder.value.id, 'reschedule', {
    startsAt: newStart.value,
    reason: rescheduleReason.value
  })
  rescheduling.value = false
  ElMessage.success('预约已调整，符合产能的人员已重新安排')
  await load()
}
async function showOrder(id: number) {
  detail.value = await api.getOrderDetail(id)
  paymentEntries.value = can('payment:read') ? await api.getPaymentEntries(id) : []
  detailVisible.value = true
  detailPhotos.value.forEach((p) => URL.revokeObjectURL(p.url))
  detailPhotos.value = []
  for (const photo of await api.getOrderEvidence(id)) {
    const blob = await api.evidenceImage(id, photo.id)
    detailPhotos.value.push({ ...photo, url: URL.createObjectURL(blob) })
  }
}
onBeforeUnmount(() => detailPhotos.value.forEach((p) => URL.revokeObjectURL(p.url)))
const statusLabel = (s: string) => labels[s] || s
async function load() {
  loading.value = true
  try {
    const data = isCatalog.value
      ? await api.listCatalog(tab.value, { page: page.value, size: 20 })
      : await api.listBusiness(tab.value, { page: page.value, size: 20 })
    rows.value = Array.isArray(data) ? data : data.list
    total.value = Array.isArray(data) ? data.length : data.total
  } finally {
    loading.value = false
  }
}
async function edit(row?: api.BusinessRow) {
  stores.value = (await api.listCatalog('stores', { size: 100 })).list
  form.value = row
    ? {
        ...row,
        storeId: row.store_id,
        serviceArea: row.service_area,
        priceYuan: row.price_cents / 100,
        durationMinutes: row.duration_minutes
      }
    : {
        name: '',
        status: 'ACTIVE',
        priceYuan: 100,
        durationMinutes: 60,
        category: '家政服务',
        version: 0
      }
  editing.value = true
}
async function save() {
  if (!(await formRef.value?.validate())) return
  saving.value = true
  try {
    await api.saveCatalog(tab.value, {
      ...form.value,
      priceCents:
        tab.value === 'services' ? Math.round((form.value.priceYuan || 0) * 100) : undefined
    })
    ElMessage.success('已保存')
    editing.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function action(row: api.BusinessRow, name: string) {
  await ElMessageBox.confirm('确认执行此订单操作？', '订单确认')
  await api.orderAction(row.id, name)
  await load()
}
async function beginAssign(row: api.BusinessRow) {
  selectedOrder.value = row
  workers.value = (await api.listCatalog('workers', { size: 100 })).list.filter(
    (w: api.BusinessRow) => w.store_id === row.store_id && w.status === 'ACTIVE'
  )
  workerId.value = undefined
  assigning.value = true
}
async function assign() {
  await api.orderAction(selectedOrder.value.id, 'assign', { workerId: workerId.value })
  assigning.value = false
  await load()
}
async function refund(row: api.BusinessRow, name: string) {
  if (name === 'approve')
    await ElMessageBox.confirm(`确认退款 ¥${money(row.amount_cents)}？`, '退款确认')
  await api.refundAction(row.id, name)
  await load()
}
onMounted(async () => {
  await loadAccess()
  if (!tabs.value.some((t) => t.key === tab.value)) tab.value = tabs.value[0]?.key || ''
  if (tab.value) await load()
})
</script>
<style scoped>
.detail-timeline {
  margin-top: 28px;
}
.evidence-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}
.evidence-grid figure {
  margin: 0;
}
.evidence-grid .el-image {
  width: 100%;
  height: 180px;
  border-radius: 12px;
}
.evidence-grid figcaption {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
}
.hm-operations {
  padding: 28px;
  background: var(--el-bg-color);
  border-radius: 8px;
}
.hm-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 30px;
}
.hm-heading p {
  font-size: 12px;
  letter-spacing: 2px;
  color: #136f63;
  margin: 0 0 10px;
}
.hm-heading h1 {
  font-size: 25px;
  font-weight: 600;
  margin: 0;
  line-height: 1.5;
}
.hm-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px 0;
  color: var(--el-text-color-secondary);
}
.hm-pagination {
  justify-content: flex-end;
  margin-top: 24px;
}
.hm-form-row {
  display: flex;
  gap: 28px;
}
@media (max-width: 700px) {
  .hm-operations {
    padding: 16px;
  }
  .hm-heading h1 {
    font-size: 20px;
  }
}
</style>
