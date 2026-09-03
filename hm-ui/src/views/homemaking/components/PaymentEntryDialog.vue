<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(560px, 94vw)"
    :close-on-click-modal="false"
  >
    <el-alert
      :title="
        kind === 'REVERSAL'
          ? '冲正用于撤销误登记，不代表向客户转账。已开始服务或退款的订单请走售后。'
          : kind === 'REFUND'
            ? '请先通过所选渠道完成实际退款，再登记凭证。此操作不会自动转账。'
            : '请核实款项实际到账后再确认。V1 按整单金额确认，不能登记为免费或部分付款。'
      "
      type="warning"
      :closable="false"
    />
    <el-form label-position="top" class="payment-entry-form">
      <el-form-item label="订单编号">HM-{{ target?.order_id || target?.id }}</el-form-item>
      <el-form-item
        v-if="kind !== 'REVERSAL'"
        :label="kind === 'REFUND' ? '实际退款渠道' : '实际收款渠道'"
        ><el-select v-model="form.channel"
          ><el-option
            v-for="(label, key) in offlineChannels"
            :key="key"
            :value="key"
            :label="label" /></el-select
      ></el-form-item>
      <el-form-item label="金额（元）"
        ><el-input-number :model-value="amount / 100" :precision="2" disabled
      /></el-form-item>
      <el-form-item label="实际发生时间"
        ><el-date-picker
          v-model="form.occurredAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          :clearable="false"
      /></el-form-item>
      <el-form-item label="凭证及备注（必填）"
        ><el-input
          v-model="form.note"
          type="textarea"
          maxlength="1000"
          show-word-limit
          placeholder="填写流水号、核对情况或冲正原因，请勿填写密码、密钥"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button :disabled="saving" @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="submit">{{ title }}</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/homemaking'
import { paymentChannels } from '../paymentLabels'
const visible = defineModel<boolean>({ required: true })
const props = defineProps<{ kind: 'RECEIPT' | 'REFUND' | 'REVERSAL'; target?: api.BusinessRow }>()
const emit = defineEmits<{ saved: [] }>()
const saving = ref(false),
  form = ref({ channel: 'CASH', occurredAt: '', note: '', requestKey: '' })
const offlineChannels = Object.fromEntries(
  Object.entries(paymentChannels).filter(([key]) => key !== 'WX_MINI')
)
const title = computed(
  () => ({ RECEIPT: '确认线下收款', REFUND: '登记线下退款', REVERSAL: '确认收款冲正' })[props.kind]
)
const amount = computed(() =>
  props.kind === 'RECEIPT'
    ? Number(props.target?.price_cents)
    : Math.abs(Number(props.target?.amount_cents))
)
watch(visible, (open) => {
  if (open)
    form.value = {
      channel: 'CASH',
      occurredAt: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
      note: '',
      requestKey: crypto.randomUUID()
    }
})
async function submit() {
  if (!form.value.note.trim() || !form.value.occurredAt)
    return ElMessage.warning('请填写实际发生时间和凭证备注')
  await ElMessageBox.confirm(
    `${title.value} ￥${(amount.value / 100).toFixed(2)}？记录将保留在订单和收支明细中。`,
    title.value
  )
  saving.value = true
  try {
    const data = { ...form.value, note: form.value.note.trim(), amountCents: amount.value }
    if (props.kind === 'RECEIPT') await api.recordOfflineReceipt(props.target!.id, data)
    else if (props.kind === 'REFUND') await api.recordOfflineRefund(props.target!.id, data)
    else await api.reverseReceipt(props.target!.order_id, { ...data, receiptId: props.target!.id })
    ElMessage.success('收支记录已保存')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.payment-entry-form {
  margin-top: 22px;
}
</style>
