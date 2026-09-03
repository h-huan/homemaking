<template>
  <el-drawer
    v-model="visible"
    :title="`${service?.name || '服务'} · 价格与预约规则`"
    size="min(900px,98vw)"
  >
    <div v-loading="loading" v-if="form">
      <el-alert title="修改只影响后续报价和预约，已有订单保留成交价格与时间。" :closable="false" />
      <el-tabs>
        <el-tab-pane v-for="kind in optionKinds" :key="kind.key" :label="kind.label">
          <p class="hint">{{
            kind.key === 'skus'
              ? '每种规格可独立设置价格和时长；不配置时使用服务基础价格。'
              : '加项按客户选择数量收费，不增加服务时长。'
          }}</p>
          <div v-for="(item, index) in form[kind.key]" :key="index" class="option-row">
            <el-input v-model="item.name" placeholder="名称" maxlength="128" aria-label="名称" />
            <el-input-number
              v-model="item.yuan"
              :min="kind.key === 'skus' ? 0.01 : 0"
              :max="1000000"
              :precision="2"
              aria-label="价格（元）"
            />
            <el-input-number
              v-if="kind.key === 'skus'"
              v-model="item.durationMinutes"
              :min="30"
              :max="480"
              :step="30"
              step-strictly
              aria-label="时长（分钟）"
            />
            <el-switch v-model="item.enabled" active-text="启用" />
            <el-button type="danger" link @click="form[kind.key].splice(index, 1)">移除</el-button>
          </div>
          <el-button
            @click="form[kind.key].push({ name: '', yuan: 0, durationMinutes: 60, enabled: true })"
            >添加{{ kind.label }}</el-button
          >
          <p class="hint">价格单位为元，时长单位为分钟。移除的历史条目会停用，保留订单追溯。</p>
        </el-tab-pane>
        <el-tab-pane label="服务区域">
          <p class="hint"
            >行政区代码须与客户地址、人员区域一致。未配置时不额外限制区域，仍检查人员服务范围。每个区域可设置附加费用。</p
          >
          <div v-for="(area, index) in form.areas" :key="index" class="area-row">
            <el-input
              v-model="area.name"
              placeholder="区域名称"
              maxlength="128"
              aria-label="区域名称"
            />
            <el-input
              v-model="area.districtCode"
              placeholder="行政区代码"
              maxlength="20"
              aria-label="行政区代码"
            />
            <el-input-number
              v-model="area.yuan"
              :min="0"
              :max="1000000"
              :precision="2"
              aria-label="区域附加费（元）"
            />
            <el-button type="danger" link @click="form.areas.splice(index, 1)">移除</el-button>
          </div>
          <el-button @click="form.areas.push({ name: '', districtCode: '', yuan: 0 })"
            >添加区域</el-button
          >
        </el-tab-pane>
        <el-tab-pane label="预约与变更规则">
          <el-form label-position="top" class="rules">
            <el-form-item label="至少提前预约（分钟）"
              ><el-input-number v-model="form.rule.minAdvanceMinutes" :min="0" :max="10080"
            /></el-form-item>
            <el-form-item label="最远可约（天）"
              ><el-input-number v-model="form.rule.maxAdvanceDays" :min="1" :max="90"
            /></el-form-item>
            <el-form-item label="允许当天预约"
              ><el-switch v-model="form.rule.allowSameDay"
            /></el-form-item>
            <el-form-item label="可预约开始时刻（仍须有人员排班）" class="wide"
              ><el-select v-model="form.rule.starts" multiple class="w-full"
                ><el-option
                  v-for="time in times"
                  :key="time"
                  :label="time"
                  :value="time" /></el-select
            ></el-form-item>
            <el-form-item label="允许客户自助取消"
              ><el-switch v-model="form.rule.allowCancel"
            /></el-form-item>
            <el-form-item label="取消须提前（分钟）"
              ><el-input-number v-model="form.rule.cancelBeforeMinutes" :min="0" :max="10080"
            /></el-form-item>
            <el-form-item label="允许客户自助改期（最多两次）"
              ><el-switch v-model="form.rule.allowReschedule"
            /></el-form-item>
            <el-form-item label="改期须提前（分钟）"
              ><el-input-number v-model="form.rule.rescheduleBeforeMinutes" :min="0" :max="10080"
            /></el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
    <template #footer
      ><el-button @click="visible = false">关闭</el-button
      ><el-button type="primary" :disabled="loading || !form" :loading="saving" @click="save"
        >保存配置</el-button
      ></template
    >
  </el-drawer>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api/homemaking'
const visible = defineModel<boolean>({ default: false })
const props = defineProps<{ service?: api.BusinessRow }>(),
  emit = defineEmits(['saved'])
const form = ref<api.BusinessRow>(),
  loading = ref(false),
  saving = ref(false)
const optionKinds = [
  { key: 'skus', label: '规格' },
  { key: 'extras', label: '加项' }
]
const times = Array.from({ length: 26 }, (_, index) => {
  const i = index + 16
  return `${String(Math.floor(i / 2)).padStart(2, '0')}:${i % 2 ? '30' : '00'}`
})
watch(visible, async (open) => {
  if (!open || !props.service) return
  form.value = undefined
  loading.value = true
  try {
    const result = await api.getServiceSettings(props.service.id)
    for (const key of ['skus', 'extras', 'areas'])
      result[key] = result[key].map((item: api.BusinessRow) => ({
        ...item,
        yuan: (item.priceCents ?? item.extraCents) / 100
      }))
    form.value = result
  } finally {
    loading.value = false
  }
})
async function save() {
  if (!form.value || !props.service || saving.value) return
  const data = JSON.parse(JSON.stringify(form.value))
  for (const key of ['skus', 'extras', 'areas'])
    for (const item of data[key]) {
      if (!item.name?.trim() || !Number.isFinite(item.yuan))
        return ElMessage.warning('请填写名称和金额')
      item[key === 'areas' ? 'extraCents' : 'priceCents'] = Math.round(item.yuan * 100)
      delete item.yuan
    }
  if (!data.rule.starts.length) return ElMessage.warning('请选择至少一个可预约时刻')
  saving.value = true
  try {
    await api.saveServiceSettings(props.service.id, data)
    ElMessage.success('价格与预约规则已保存')
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
<style scoped>
.hint {
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin: 18px 0;
}
.option-row,
.area-row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin: 14px 0;
}
.option-row > .el-input,
.area-row > .el-input {
  min-width: 120px;
  flex: 1;
}
.rules {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 24px;
  padding-top: 20px;
}
.wide {
  grid-column: 1/-1;
}
@media (max-width: 640px) {
  .option-row,
  .area-row {
    flex-wrap: wrap;
    border-bottom: 1px solid var(--el-border-color);
    padding-bottom: 16px;
  }
  .rules {
    display: block;
  }
}
</style>
