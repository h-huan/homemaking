<template>
  <el-dialog
    v-model="visible"
    title="变更订单约定"
    width="min(660px, 94vw)"
    :close-on-click-modal="false"
  >
    <p class="change-intro"
      >修改前请与客户确认。涉及已付款差额时，原约定保留至补款或退款登记完成。</p
    >
    <el-form label-position="top" :disabled="saving">
      <div class="change-grid">
        <el-form-item label="联系人"
          ><el-input v-model="form.contactName" maxlength="100"
        /></el-form-item>
        <el-form-item label="联系电话"
          ><el-input v-model="form.phone" maxlength="32"
        /></el-form-item>
      </div>
      <el-form-item label="服务地址"
        ><el-input v-model="form.address" maxlength="500"
      /></el-form-item>
      <el-form-item label="行政区划代码"
        ><el-input
          v-model="form.districtCode"
          maxlength="20"
          placeholder="与服务区域设置一致，用于检查服务范围和区域费用"
      /></el-form-item>
      <el-checkbox v-if="canPrice" v-model="overridePrice">指定协商后的订单总价</el-checkbox>
      <el-form-item v-if="canPrice && overridePrice" label="新总价（元，含区域服务费）"
        ><el-input-number v-model="form.amount" :precision="2" :min="0.01" :max="1000000"
      /></el-form-item>
      <el-form-item label="变更原因及客户确认情况"
        ><el-input v-model="form.reason" type="textarea" maxlength="500" show-word-limit
      /></el-form-item>
    </el-form>
    <el-descriptions v-if="preview" :column="2" border>
      <el-descriptions-item label="原价"
        >￥{{ money(preview.before.price_cents) }}</el-descriptions-item
      >
      <el-descriptions-item label="新价"
        >￥{{ money(preview.after.price_cents) }}</el-descriptions-item
      >
      <el-descriptions-item label="款项处理" :span="2">{{ outcome }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button :loading="checking" :disabled="saving" @click="checkQuote">核对变更</el-button>
      <el-button type="primary" :loading="saving" :disabled="!preview || checking" @click="submit"
        >确认变更</el-button
      >
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api/homemaking'
const visible = defineModel<boolean>({ required: true })
const props = defineProps<{ order?: api.BusinessRow; canPrice: boolean }>()
const emit = defineEmits<{ saved: [] }>()
const form = ref({
  contactName: '',
  phone: '',
  address: '',
  districtCode: '',
  amount: 0,
  reason: ''
})
const overridePrice = ref(false),
  preview = ref<api.BusinessRow>(),
  checking = ref(false),
  saving = ref(false)
let requestKey = '',
  sequence = 0,
  checkedRequest: api.BusinessRow | undefined
const money = (cents: number) => (Number(cents) / 100).toFixed(2)
const outcome = computed(() =>
  !preview.value?.requiresSettlement
    ? Number(props.order?.paid_cents) > 0
      ? '确认后直接生效，无需再次付款'
      : '确认后直接生效，按新价收款'
    : preview.value.differenceCents > 0
      ? `需补款 ￥${money(preview.value.differenceCents)}，到账登记后生效`
      : `需退回 ￥${money(-preview.value.differenceCents)}，财务登记后生效`
)
watch(visible, (open) => {
  sequence++
  preview.value = undefined
  checkedRequest = undefined
  if (open && props.order) {
    const o = props.order
    form.value = {
      contactName: o.contact_name,
      phone: o.phone,
      address: o.address,
      districtCode: o.district_code || '',
      amount: Number(o.price_cents) / 100,
      reason: ''
    }
    overridePrice.value = false
    requestKey = crypto.randomUUID()
  }
})
watch(
  [form, overridePrice],
  () => {
    sequence++
    preview.value = undefined
    checkedRequest = undefined
  },
  { deep: true }
)
async function checkQuote() {
  if (
    !form.value.reason.trim() ||
    !form.value.contactName.trim() ||
    !form.value.phone.trim() ||
    !form.value.address.trim()
  )
    return ElMessage.warning('请填写联系人、地址及变更原因')
  const current = ++sequence
  checking.value = true
  const body = {
    version: props.order!.version,
    contact: {
      contactName: form.value.contactName.trim(),
      phone: form.value.phone.trim(),
      address: form.value.address.trim(),
      districtCode: form.value.districtCode.trim()
    },
    priceCents: props.canPrice && overridePrice.value ? Math.round(form.value.amount * 100) : null,
    reason: form.value.reason.trim(),
    requestKey
  }
  try {
    const result = await api.previewOrderChange(props.order!.id, body)
    if (current === sequence && visible.value) {
      preview.value = result
      checkedRequest = { ...body, expectedPriceCents: result.after.price_cents }
    }
  } finally {
    checking.value = false
  }
}
async function submit() {
  if (!checkedRequest || !preview.value || saving.value) return
  saving.value = true
  try {
    await api.createOrderChange(props.order!.id, checkedRequest)
    ElMessage.success('订单变更已登记')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.change-intro {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin: 0 0 20px;
}
.change-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
@media (max-width: 540px) {
  .change-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
