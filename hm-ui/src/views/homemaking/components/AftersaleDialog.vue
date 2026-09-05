<template>
  <el-dialog v-model="visible" title="售后处理" width="min(720px, 94vw)" destroy-on-close>
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="detail">
      <el-descriptions :column="descriptionColumns" border>
        <el-descriptions-item label="售后类型">{{ typeLabel(detail.type) }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">{{
          statusLabel(detail.status)
        }}</el-descriptions-item>
        <el-descriptions-item label="订单编号">{{ detail.order_id }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{
          detail.amount_cents ? `¥${money(detail.amount_cents)}` : '不涉及退款'
        }}</el-descriptions-item>
        <el-descriptions-item label="客户诉求" :span="descriptionColumns">{{
          detail.reason
        }}</el-descriptions-item>
        <el-descriptions-item
          v-if="detail.resolution"
          label="处理结果"
          :span="descriptionColumns"
          >{{ detail.resolution }}</el-descriptions-item
        >
        <el-descriptions-item v-if="detail.scheduled_at" label="补救预约" :span="descriptionColumns"
          >{{ formatDate(detail.scheduled_at) }} · 人员 #{{
            detail.assigned_worker_id
          }}</el-descriptions-item
        >
      </el-descriptions>
      <el-form
        v-if="canProcess && detail.status === 'REQUESTED' && remedy"
        class="process-form"
        label-position="top"
      >
        <el-form-item label="补救上门时间"
          ><el-date-picker
            v-model="form.startsAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :disabled-date="past"
        /></el-form-item>
        <el-form-item label="服务人员"
          ><el-select v-model="form.workerId" placeholder="按技能、区域和排班再次校验"
            ><el-option
              v-for="worker in workers"
              :key="worker.id"
              :label="worker.name"
              :value="worker.id" /></el-select
        ></el-form-item>
        <el-form-item label="处理说明"
          ><el-input
            v-model="form.resolution"
            maxlength="1000"
            show-word-limit
            type="textarea"
            :rows="3"
        /></el-form-item>
        <el-button type="primary" :loading="saving" @click="schedule">确认安排补救服务</el-button>
      </el-form>
      <el-form
        v-if="canProcess && detail.status === 'REQUESTED' && detail.type === 'OTHER_COMPENSATION'"
        class="process-form"
        label-position="top"
      >
        <el-form-item label="补偿内容与兑现说明"
          ><el-input
            v-model="form.resolution"
            maxlength="1000"
            show-word-limit
            type="textarea"
            :rows="3"
        /></el-form-item>
        <el-button type="primary" :loading="saving" @click="resolve">登记补偿完成</el-button>
      </el-form>
      <el-timeline class="timeline">
        <el-timeline-item
          v-for="(item, index) in detail.logs"
          :key="index"
          :timestamp="formatDate(item.created_at)"
          placement="top"
        >
          <strong>{{ statusLabel(item.to_status) }}</strong
          ><p>{{ item.detail || item.action }}</p>
        </el-timeline-item>
      </el-timeline>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import * as api from '@/api/homemaking'
const props = defineProps<{ modelValue: boolean; target?: api.BusinessRow; canProcess: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'saved'): void }>()
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
const { width } = useWindowSize()
const descriptionColumns = computed(() => (width.value < 640 ? 1 : 2))
const detail = ref<api.BusinessRow>(),
  workers = ref<api.BusinessRow[]>([]),
  loading = ref(false),
  saving = ref(false)
const form = reactive<{ startsAt?: string; workerId?: number; resolution: string }>({
  resolution: ''
})
const remedies = ['REWORK', 'REASSIGN_WORKER', 'REVISIT']
const remedy = computed(() => !!detail.value && remedies.includes(detail.value.type))
const typeLabel = (type: string) =>
  ({
    REWORK: '补做',
    REASSIGN_WORKER: '更换服务人员',
    REVISIT: '重新上门',
    PARTIAL_REFUND: '部分退款',
    FULL_REFUND: '全额退款',
    OTHER_COMPENSATION: '其他补偿'
  })[type] || type
const statusLabel = (status: string) =>
  ({
    REQUESTED: '等待处理',
    SCHEDULED: '已安排上门',
    IN_PROGRESS: '补救服务中',
    AWAITING_CONFIRMATION: '等待客户确认',
    REFUNDING: '退款处理中',
    REFUNDED: '退款完成',
    COMPLETED: '处理完成',
    REJECTED: '已驳回',
    CANCELLED: '已撤销',
    FAILED: '退款失败'
  })[status] || status
const money = (cents: number) => ((cents || 0) / 100).toFixed(2)
const past = (date: Date) => date.getTime() < Date.now() - 86400000
watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.target) return
    loading.value = true
    form.startsAt = undefined
    form.workerId = undefined
    form.resolution = ''
    try {
      const current = await api.getAftersale(props.target.id)
      detail.value = current
      if (remedies.includes(current.type))
        workers.value = ((await api.listCatalog('workers', { size: 100 })).list || []).filter(
          (worker: api.BusinessRow) =>
            worker.status === 'ACTIVE' && worker.store_id === props.target?.store_id
        )
    } finally {
      loading.value = false
    }
  }
)
async function schedule() {
  if (!form.startsAt || !form.workerId || !form.resolution.trim())
    return ElMessage.warning('请选择时间、人员并填写处理说明')
  saving.value = true
  try {
    await api.scheduleAftersale(detail.value!.id, { ...form, resolution: form.resolution.trim() })
    ElMessage.success('补救服务已安排')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
async function resolve() {
  if (!form.resolution.trim()) return ElMessage.warning('请填写补偿内容和兑现说明')
  saving.value = true
  try {
    await api.resolveAftersale(detail.value!.id, { remark: form.resolution.trim() })
    ElMessage.success('补偿结果已登记')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.process-form {
  margin-top: 24px;
  padding: 20px;
  border-radius: 16px;
  background: var(--el-fill-color-light);
}
.process-form :deep(.el-select),
.process-form :deep(.el-date-editor) {
  width: 100%;
}
.timeline {
  margin-top: 28px;
}
.timeline p {
  margin: 6px 0 0;
  color: var(--el-text-color-secondary);
}
</style>
