<template>
  <div>
    <div class="hm-toolbar"
      ><el-date-picker
        v-model="period"
        type="daterange"
        value-format="YYYY-MM-DD"
        :clearable="false"
      /><el-button :loading="loading" @click="refresh">查询收支</el-button></div
    >
    <el-descriptions :column="4" border class="ledger-summary">
      <el-descriptions-item label="收款">￥{{ money(summary.received_cents) }}</el-descriptions-item
      ><el-descriptions-item label="退款"
        >￥{{ money(summary.refunded_cents) }}</el-descriptions-item
      ><el-descriptions-item label="冲正"
        >￥{{ money(summary.reversed_cents) }}</el-descriptions-item
      ><el-descriptions-item label="净收款">￥{{ money(summary.net_cents) }}</el-descriptions-item>
    </el-descriptions>
    <el-table :data="rows" v-loading="loading" empty-text="所选日期暂无收支记录">
      <el-table-column prop="id" label="流水" width="85" /><el-table-column
        prop="order_id"
        label="订单"
        width="85"
      />
      <el-table-column label="类型" width="100"
        ><template #default="{ row }">{{ paymentKinds[row.kind] }}</template></el-table-column
      >
      <el-table-column label="渠道" min-width="120"
        ><template #default="{ row }">{{
          paymentChannels[row.channel] || row.channel
        }}</template></el-table-column
      >
      <el-table-column label="金额（元）" width="130"
        ><template #default="{ row }">{{ money(row.amount_cents) }}</template></el-table-column
      >
      <el-table-column label="实际发生时间" min-width="180">
        <template #default="{ row }">{{ formatDate(row.occurred_at) }}</template>
      </el-table-column>
      <el-table-column label="登记时间" min-width="180">
        <template #default="{ row }">{{ formatDate(row.recorded_at) }}</template>
      </el-table-column>
      <el-table-column label="操作人" min-width="130"
        ><template #default="{ row }">{{
          row.operator_name || (row.operator_id ? '#' + row.operator_id : '支付渠道回调')
        }}</template></el-table-column
      >
      <el-table-column prop="note" label="凭证与备注" min-width="220" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      :page-size="20"
      :total="Number(summary.total || 0)"
      layout="prev,pager,next,total"
      @current-change="load"
    />
    <p class="ledger-note"
      >按实际发生时间统计启用收支明细后的新流水，仅包含授权范围。历史收款可在订单和结算记录中核对。</p
    >
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import dayjs from 'dayjs'
import { paymentLedger } from '@/api/homemaking'
import { formatDate } from '@/utils/formatTime'
import { paymentChannels, paymentKinds } from '../paymentLabels'
const props = defineProps<{ tenantId: number }>()
const period = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')]),
  page = ref(1),
  loading = ref(false),
  rows = ref<any[]>([]),
  summary = ref<Record<string, any>>({})
const money = (cents: number) => ((Number(cents) || 0) / 100).toFixed(2)
let sequence = 0
async function load() {
  const current = ++sequence
  if (!Number.isInteger(props.tenantId) || props.tenantId < 1) {
    rows.value = []
    summary.value = {}
    loading.value = false
    return
  }
  loading.value = true
  try {
    const data = await paymentLedger({
      from: period.value[0],
      to: period.value[1],
      page: page.value,
      size: 20,
      tenantId: props.tenantId
    })
    if (current === sequence) {
      rows.value = data.list
      summary.value = data.summary
    }
  } finally {
    if (current === sequence) loading.value = false
  }
}
async function refresh() {
  page.value = 1
  await load()
}
onMounted(load)
watch(() => props.tenantId, refresh)
</script>
<style scoped>
.ledger-summary {
  margin: 18px 0;
}
.ledger-note {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  font-size: 13px;
}
</style>
