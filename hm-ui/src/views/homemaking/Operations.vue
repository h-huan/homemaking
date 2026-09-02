<template>
  <div class="hm-operations">
    <header class="hm-heading"
      ><div><p>HM · 日常运营</p><h1>把每一次服务，安排妥当。</h1></div
      ><el-button v-if="isCatalog" type="primary" @click="edit()"
        >新增{{ tabLabel }}</el-button
      ></header
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
      <el-table-column label="状态" width="120"
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
          <el-button v-if="isCatalog" link type="primary" @click="edit(row)">编辑</el-button>
          <template v-if="tab === 'orders'">
            <el-button
              v-if="['PAID', 'ASSIGNED'].includes(row.status)"
              link
              type="primary"
              @click="beginAssign(row)"
              >派单</el-button
            >
            <el-button
              v-if="row.status === 'ASSIGNED'"
              link
              type="primary"
              @click="action(row, 'start')"
              >开始服务</el-button
            >
            <el-button
              v-if="row.status === 'IN_SERVICE'"
              link
              type="primary"
              @click="action(row, 'complete')"
              >确认完工</el-button
            >
            <el-button
              v-if="row.status === 'UNPAID'"
              link
              type="danger"
              @click="action(row, 'cancel')"
              >取消订单</el-button
            >
          </template>
          <template v-if="tab === 'aftersales'"
            ><el-button
              v-if="row.status === 'REQUESTED'"
              link
              type="danger"
              @click="refund(row, 'approve')"
              >批准退款</el-button
            ><el-button v-if="row.status === 'REFUNDING'" link @click="refund(row, 'sync')"
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
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import * as api from '@/api/homemaking'
defineOptions({ name: 'HomemakingOperations' })
const tabs = [
  { key: 'stores', label: '门店' },
  { key: 'workers', label: '服务人员' },
  { key: 'services', label: '服务' },
  { key: 'orders', label: '订单' },
  { key: 'customers', label: '客户' },
  { key: 'aftersales', label: '售后' },
  { key: 'settlements', label: '结算' }
]
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
const tabLabel = computed(() => tabs.find((t) => t.key === tab.value)?.label || '')
const money = (cents: number) => ((cents || 0) / 100).toFixed(2)
const labels: Record<string, string> = {
  ACTIVE: '启用',
  INACTIVE: '停用',
  UNPAID: '待付款',
  PAID: '待派单',
  ASSIGNED: '待服务',
  IN_SERVICE: '服务中',
  COMPLETED: '已完工',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  REQUESTED: '待审核',
  PENDING: '待结算'
}
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
onMounted(load)
</script>
<style scoped>
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
