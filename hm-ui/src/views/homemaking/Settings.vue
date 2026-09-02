<template>
  <div class="hm-settings">
    <header><p>HM · 租户设置</p><h1>你的品牌，你的服务方式。</h1></header>
    <el-tabs v-model="tab">
      <el-tab-pane label="品牌与首页" name="brand">
        <el-form v-loading="loading" label-position="top" class="hm-form">
          <div class="hm-row"
            ><el-form-item label="品牌名称"
              ><el-input v-model="brand.brandName" maxlength="100" /></el-form-item
            ><el-form-item label="经营方式"
              ><el-select v-model="brand.operationMode"
                ><el-option label="总部直营" value="DIRECT" /><el-option
                  label="加盟运营"
                  value="FRANCHISE" /></el-select></el-form-item
          ></div>
          <el-form-item label="官网地址"
            ><el-input v-model="brand.website" placeholder="https://"
          /></el-form-item>
          <div class="hm-row"
            ><el-form-item label="Logo 地址"><el-input v-model="brand.logo" /></el-form-item
            ><el-form-item label="浏览器图标地址"><el-input v-model="brand.favicon" /></el-form-item
          ></div>
          <el-form-item label="品牌主色"
            ><el-color-picker v-model="brand.primaryColor"
          /></el-form-item>
          <el-form-item label="登录页标题"
            ><el-input v-model="brand.loginTitle" maxlength="200"
          /></el-form-item>
          <el-form-item label="登录页背景图"
            ><el-input v-model="brand.loginBackground" placeholder="HTTPS 图片地址，可留空"
          /></el-form-item>
          <el-form-item label="首页展示内容"
            ><el-checkbox-group v-model="brand.homeModules"
              ><el-checkbox v-for="m in modules" :key="m.value" :value="m.value">{{
                m.label
              }}</el-checkbox></el-checkbox-group
            ></el-form-item
          >
          <div class="hm-row"
            ><el-form-item label="小程序 AppID"><el-input v-model="brand.miniAppId" /></el-form-item
            ><el-form-item label="公众号 AppID"><el-input v-model="brand.mpAppId" /></el-form-item
          ></div>
          <el-form-item label="支付应用标识"><el-input v-model="brand.payAppKey" /></el-form-item>
          <el-button type="primary" :loading="saving" @click="saveBrand">保存品牌设置</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="域名" name="domain"
        ><div class="hm-form"
          ><p>填写域名后，按提示添加 DNS TXT 记录。验证通过后启用品牌识别。</p
          ><el-input v-model="domain" placeholder="例如 service.example.com" /><div
            class="hm-actions"
            ><el-button @click="requestDomain">获取验证记录</el-button
            ><el-button type="primary" @click="verifyDomain">验证域名</el-button></div
          ><el-descriptions v-if="domainProof.record" :column="1" border
            ><el-descriptions-item label="记录名">{{ domainProof.record }}</el-descriptions-item
            ><el-descriptions-item label="记录值">{{
              domainProof.value
            }}</el-descriptions-item></el-descriptions
          ></div
        ></el-tab-pane
      >
      <el-tab-pane label="通知规则" name="notifications"
        ><div class="hm-form">
          <el-alert
            title="通知同时受平台上限、租户规则和客户偏好约束。普通消息在静默时段延后发送。"
            type="info"
            :closable="false"
          />
          <el-form label-position="top"
            ><el-form-item label="消息类型"
              ><el-select v-model="event" @change="loadPolicy"
                ><el-option
                  v-for="e in events"
                  :key="e.value"
                  :label="e.label"
                  :value="e.value" /></el-select
            ></el-form-item>
            <el-form-item label="启用通知"><el-switch v-model="policy.enabled" /></el-form-item>
            <div class="hm-row"
              ><el-form-item :label="`每日最多条数（平台上限 ${platform.dailyLimit ?? '—'}）`"
                ><el-input-number
                  v-model="policy.dailyLimit"
                  :min="0"
                  :max="platform.dailyLimit || 100" /></el-form-item
              ><el-form-item label="最短通知间隔（分钟）"
                ><el-input-number
                  v-model="policy.minIntervalMinutes"
                  :min="platform.minIntervalMinutes || 0"
                  :max="1440" /></el-form-item
            ></div>
            <div class="hm-row"
              ><el-form-item label="静默开始时间"
                ><el-select v-model="policy.quietStart"
                  ><el-option
                    v-for="h in 24"
                    :key="h"
                    :label="`${h - 1}:00`"
                    :value="h - 1" /></el-select></el-form-item
              ><el-form-item label="静默结束时间"
                ><el-select v-model="policy.quietEnd"
                  ><el-option
                    v-for="h in 24"
                    :key="h"
                    :label="`${h - 1}:00`"
                    :value="h - 1" /></el-select></el-form-item
            ></div>
            <el-form-item label="通知渠道（按顺序尝试）"
              ><el-checkbox-group v-model="policy.channels"
                ><el-checkbox value="MP">公众号</el-checkbox
                ><el-checkbox value="MINI">小程序</el-checkbox
                ><el-checkbox value="SMS" :disabled="!platform.allowSms"
                  >短信</el-checkbox
                ></el-checkbox-group
              ></el-form-item
            >
            <el-form-item label="允许重要通知使用短信兜底"
              ><el-switch v-model="policy.allowSms" :disabled="!platform.allowSms"
            /></el-form-item>
            <el-button type="primary" @click="savePolicy">保存通知规则</el-button>
          </el-form>
        </div></el-tab-pane
      >
      <el-tab-pane label="通知记录" name="history"
        ><el-button @click="loadHistory">刷新记录</el-button
        ><el-table :data="history" empty-text="暂无通知记录"
          ><el-table-column prop="id" label="编号" width="80" /><el-table-column
            prop="event_type"
            label="事件" /><el-table-column prop="customer_id" label="客户编号" /><el-table-column
            prop="status"
            label="状态" /><el-table-column prop="sent_channel" label="渠道" /><el-table-column
            prop="last_error"
            label="处理说明"
            min-width="260" /></el-table
      ></el-tab-pane>
    </el-tabs>
  </div>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api/homemaking'
import { applyTenantBrand } from '@/hooks/web/useTenantBrand'
defineOptions({ name: 'HomemakingSettings' })
const tab = ref('brand'),
  loading = ref(false),
  saving = ref(false),
  event = ref('*'),
  domain = ref('')
const brand = ref<api.BusinessRow>({ homeModules: [] }),
  policy = ref<api.BusinessRow>({ channels: [] }),
  platform = ref<api.BusinessRow>({}),
  domainProof = ref<api.BusinessRow>({}),
  history = ref<api.BusinessRow[]>([])
const modules = [
  { value: 'services', label: '服务' },
  { value: 'stores', label: '门店' },
  { value: 'workers', label: '服务人员' },
  { value: 'reviews', label: '客户评价' },
  { value: 'contact', label: '联系我们' },
  { value: 'banners', label: '轮播内容' }
]
const events = [
  { value: '*', label: '默认规则' },
  { value: 'SERVICE_REMINDER', label: '服务提醒' },
  { value: 'WORKER_CHANGED', label: '服务人员变更' },
  { value: 'ORDER_CANCELLED', label: '订单取消' },
  { value: 'REFUND_RESULT', label: '退款结果' },
  { value: 'SERVICE_COMPLETED', label: '服务完工' },
  { value: 'MARKETING', label: '优惠活动' }
]
async function loadBrand() {
  loading.value = true
  try {
    const raw = await api.getBrand()
    applyTenantBrand(raw)
    const b: api.BusinessRow = {}
    for (const [k, v] of Object.entries(raw))
      b[k.replace(/_([a-z])/g, (_, c: string) => c.toUpperCase())] = v
    b.homeModules =
      typeof b.homeModules === 'string' ? JSON.parse(b.homeModules) : b.homeModules || []
    brand.value = b
  } finally {
    loading.value = false
  }
}
async function saveBrand() {
  saving.value = true
  try {
    await api.saveBrand(brand.value)
    await loadBrand()
    applyTenantBrand(await api.getBrand())
    ElMessage.success('品牌设置已保存')
  } finally {
    saving.value = false
  }
}
async function requestDomain() {
  domainProof.value = await api.requestDomain(domain.value)
}
async function verifyDomain() {
  await api.verifyDomain(domain.value)
  ElMessage.success('域名验证通过')
}
async function loadPolicy() {
  const data = await api.getPolicy(event.value)
  platform.value = data.platform
  policy.value = data.tenant
}
async function savePolicy() {
  await api.savePolicy(event.value, policy.value)
  ElMessage.success('通知规则已保存')
}
async function loadHistory() {
  history.value = await api.listBusiness('notifications')
}
onMounted(async () => {
  await Promise.all([loadBrand(), loadPolicy(), loadHistory()])
})
</script>
<style scoped>
.hm-settings {
  padding: 28px;
  background: var(--el-bg-color);
  border-radius: 8px;
}
.hm-settings header {
  margin-bottom: 30px;
}
.hm-settings header p {
  font-size: 12px;
  letter-spacing: 2px;
  color: #136f63;
}
.hm-settings h1 {
  font-size: 25px;
  font-weight: 600;
}
.hm-form {
  max-width: 740px;
  padding: 20px 0;
}
.hm-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}
.hm-actions {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}
.hm-form .el-alert {
  margin-bottom: 24px;
}
@media (max-width: 700px) {
  .hm-settings {
    padding: 16px;
  }
  .hm-row {
    grid-template-columns: 1fr;
    gap: 0;
  }
}
</style>
