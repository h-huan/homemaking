<template>
  <el-table :data="rows" row-key="id" empty-text="所选日期暂无请假或休息申请">
    <el-table-column type="expand">
      <template #default="{ row }">
        <el-timeline class="hm-timeoff-history">
          <el-timeline-item
            v-for="event in row.history"
            :key="event.id"
            :timestamp="formatDate(event.created_at)"
          >
            {{ actions[event.action] || event.action }} · 操作人 #{{ event.operator_id }} ·
            {{ event.reason }}
          </el-timeline-item>
        </el-timeline>
        <el-empty v-if="!row.history?.length" description="历史登记未保存操作记录" />
      </template>
    </el-table-column>
    <el-table-column label="类型" width="80"
      ><template #default="{ row }">{{
        row.kind === 'REST' ? '休息' : '请假'
      }}</template></el-table-column
    >
    <el-table-column label="申请时段" min-width="190"
      ><template #default="{ row }"
        >{{ formatDate(row.starts_at) }}<br />{{ formatDate(row.ends_at) }}</template
      ></el-table-column
    >
    <el-table-column label="状态" width="100"
      ><template #default="{ row }"
        ><el-tag
          :type="
            row.status === 'APPROVED' ? 'success' : row.status === 'PENDING' ? 'warning' : 'info'
          "
          >{{ states[row.status] || row.status }}</el-tag
        ></template
      ></el-table-column
    >
    <el-table-column prop="reason" label="申请原因" min-width="150" show-overflow-tooltip />
    <el-table-column label="处理说明" min-width="150" show-overflow-tooltip
      ><template #default="{ row }">{{
        row.cancel_reason || row.review_note || '—'
      }}</template></el-table-column
    >
    <el-table-column v-if="canReview || canCancel" label="操作" min-width="175" fixed="right">
      <template #default="{ row }">
        <el-button
          v-if="canReview && row.status === 'PENDING'"
          text
          type="primary"
          @click="$emit('review', row, true)"
          >批准</el-button
        >
        <el-button
          v-if="canReview && row.status === 'PENDING'"
          text
          type="danger"
          @click="$emit('review', row, false)"
          >驳回</el-button
        >
        <el-button v-if="canCancel && row.can_cancel" text @click="$emit('cancel', row)"
          >撤销</el-button
        >
      </template>
    </el-table-column>
  </el-table>
</template>
<script setup lang="ts">
import { formatDate } from '@/utils/formatTime'
defineProps<{ rows: any[]; canReview?: boolean; canCancel?: boolean }>()
defineEmits<{ review: [row: any, approved: boolean]; cancel: [row: any] }>()
const states: Record<string, string> = {
  PENDING: '待审核',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  CANCELLED: '已撤销'
}
const actions: Record<string, string> = {
  REQUEST: '提交申请',
  REGISTER: '管理员登记',
  APPROVE: '批准申请',
  REJECT: '驳回申请',
  CANCEL: '撤销申请'
}
</script>
<style scoped>
.hm-timeoff-history {
  margin: 20px;
  overflow-wrap: anywhere;
}
</style>
