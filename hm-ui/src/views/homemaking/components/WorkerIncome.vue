<template>
  <el-space wrap class="mb-20px">
    <el-date-picker
      v-model="range"
      type="daterange"
      value-format="YYYY-MM-DD"
      :clearable="false"
      @change="reset"
    />
    <el-button @click="load">刷新收入</el-button>
  </el-space>
  <el-alert
    v-if="!data.commissionConfigured"
    title="门店尚未启用 Worker 佣金规则；仅展示已有真实收入记录，如有疑问请联系财务。"
    type="info"
    :closable="false"
    class="mb-20px"
  />
  <el-row :gutter="20" class="mb-20px">
    <el-col :xs="24" :sm="8"
      ><el-statistic
        title="所选日期净收入（元）"
        :value="Number(data.summary?.range_earned_cents || 0) / 100"
        :precision="2"
    /></el-col>
    <el-col :xs="24" :sm="8"
      ><el-statistic
        title="累计待结算（元）"
        :value="Number(data.summary?.pending_cents || 0) / 100"
        :precision="2"
    /></el-col>
    <el-col :xs="24" :sm="8"
      ><el-statistic
        title="累计已结算（元）"
        :value="Number(data.summary?.paid_cents || 0) / 100"
        :precision="2"
    /></el-col>
  </el-row>
  <p class="hm-hint"
    >收入按佣金流水入账日期统计，退款冲正显示为负数。待结算包含全部未付款流水；已结算历史按实际付款日期筛选。</p
  >
  <el-radio-group v-model="view" class="mb-20px" @change="reset"
    ><el-radio-button value="ENTRIES">收入明细</el-radio-button
    ><el-radio-button value="PENDING">待结算</el-radio-button
    ><el-radio-button value="PAID">已结算历史</el-radio-button></el-radio-group
  >
  <el-table :data="data.list" empty-text="暂无符合条件的收入记录">
    <el-table-column v-if="view !== 'PAID'" prop="order_id" label="订单" width="85" />
    <el-table-column v-if="view !== 'PAID'" prop="service_name" label="服务" min-width="150" />
    <el-table-column v-if="view === 'PAID'" prop="id" label="结算单" width="90" />
    <el-table-column label="金额（元）" min-width="120"
      ><template #default="{ row }">{{
        (Number(row.amount_cents) / 100).toFixed(2)
      }}</template></el-table-column
    >
    <el-table-column label="状态" min-width="120"
      ><template #default="{ row }"
        >{{ states[row.statement_status || row.status] || row.status
        }}{{ row.amount_cents < 0 ? ' · 冲正' : '' }}</template
      ></el-table-column
    >
    <el-table-column v-if="view !== 'PAID'" label="入账时间" min-width="180"
      ><template #default="{ row }">{{ formatDate(row.created_at) }}</template></el-table-column
    >
    <el-table-column v-if="view === 'PAID'" label="结算区间" min-width="210"
      ><template #default="{ row }"
        >{{ formatDate(row.period_start, 'YYYY-MM-DD') }} —
        {{ formatDate(row.period_end, 'YYYY-MM-DD') }}</template
      ></el-table-column
    >
    <el-table-column v-if="view === 'PAID'" label="付款时间" min-width="180"
      ><template #default="{ row }">{{ formatDate(row.paid_at) }}</template></el-table-column
    >
    <el-table-column
      v-if="view === 'PAID'"
      prop="payment_reference"
      label="付款凭据"
      min-width="150"
      show-overflow-tooltip
    />
  </el-table>
  <el-pagination
    class="mt-20px"
    v-model:current-page="page"
    :page-size="20"
    :total="data.total || 0"
    layout="prev,pager,next"
    @current-change="load"
  />
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import * as api from '@/api/homemaking'
import { formatDate } from '@/utils/formatTime'
const range = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
const data = ref<any>({}),
  view = ref('ENTRIES'),
  page = ref(1)
const states: Record<string, string> = {
  UNBILLED: '待出结算单',
  DRAFT: '待审核',
  APPROVED: '待付款',
  PAID: '已付款',
  RECONCILED: '已对账'
}
let loadSequence = 0
async function load() {
  const sequence = ++loadSequence
  const result = await api.workerIncome({
    from: range.value[0],
    to: range.value[1],
    view: view.value,
    page: page.value,
    size: 20
  })
  if (sequence === loadSequence) data.value = result
}
async function reset() {
  page.value = 1
  await load()
}
onMounted(load)
</script>
