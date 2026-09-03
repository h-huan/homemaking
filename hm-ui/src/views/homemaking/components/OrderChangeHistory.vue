<template>
  <section class="order-changes" v-if="order.changes?.length">
    <h3>订单变更记录</h3>
    <el-alert
      v-if="order.pending_change"
      :closable="false"
      type="warning"
      :title="
        order.pending_change.status === 'PENDING_PAYMENT'
          ? '待确认补款，结清后新约定生效'
          : '待财务登记退差额，原约定暂时保留'
      "
    />
    <el-collapse>
      <el-collapse-item v-for="change in order.changes" :key="change.id" :name="change.id">
        <template #title
          ><span
            >#{{ change.id }} · {{ statuses[change.status] }} ·
            {{ formatDate(change.created_at) }}</span
          ></template
        >
        <el-descriptions :column="1" border>
          <el-descriptions-item label="价格"
            >￥{{ money(change.old_price_cents) }} → ￥{{
              money(change.new_price_cents)
            }}</el-descriptions-item
          >
          <el-descriptions-item label="原地址"
            >{{ change.before.contact_name }} · {{ change.before.phone }} ·
            {{ change.before.address }}</el-descriptions-item
          >
          <el-descriptions-item label="新地址"
            >{{ change.after.contact_name }} · {{ change.after.phone }} ·
            {{ change.after.address }}</el-descriptions-item
          >
          <el-descriptions-item label="原预约"
            >{{ formatDate(change.before.starts_at) }} —
            {{ formatDate(change.before.ends_at) }}</el-descriptions-item
          >
          <el-descriptions-item label="新预约"
            >{{ formatDate(change.after.starts_at) }} —
            {{ formatDate(change.after.ends_at) }}</el-descriptions-item
          >
          <el-descriptions-item label="原因">{{ change.reason }}</el-descriptions-item>
          <el-descriptions-item label="操作人"
            >{{ change.actor_type === 1 ? '客户' : '员工' }} #{{
              change.actor_id
            }}</el-descriptions-item
          >
          <el-descriptions-item v-if="change.cancel_reason" label="撤销记录"
            >{{ change.cancel_reason }} ·
            {{ formatDate(change.cancelled_at) }}</el-descriptions-item
          >
        </el-descriptions>
        <el-button
          v-if="canCancel && order.pending_change_id === change.id"
          class="cancel-change"
          @click="$emit('cancel', change.id)"
          >撤销未生效变更</el-button
        >
      </el-collapse-item>
    </el-collapse>
  </section>
</template>
<script setup lang="ts">
import { formatDate } from '@/utils/formatTime'
import type { BusinessRow } from '@/api/homemaking'
defineProps<{ order: BusinessRow; canCancel: boolean }>()
defineEmits<{ cancel: [id: number] }>()
const money = (cents: number) => (Number(cents) / 100).toFixed(2)
const statuses: Record<string, string> = {
  PENDING_PAYMENT: '待补款',
  PENDING_REFUND: '待退差额',
  APPLIED: '已生效',
  CANCELLED: '已撤销'
}
</script>
<style scoped>
.order-changes {
  margin-top: 24px;
}
.cancel-change {
  margin-top: 14px;
}
</style>
