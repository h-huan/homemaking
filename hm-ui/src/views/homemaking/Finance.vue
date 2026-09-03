<template>
  <HmPage
    eyebrow="HM · SaaS 与财务"
    title="租户额度和加盟结算"
    description="查看资源使用情况，设置租户功能，并逐步完成结算审核、打款登记与对账。"
  >
    <div v-if="isHeadquarters" class="tenant-picker"
      ><el-input-number v-model="tenantId" :min="1" /><el-button
        type="primary"
        :loading="loading"
        @click="load"
        >读取租户配置</el-button
      ></div
    >
    <el-tabs>
      <el-tab-pane v-if="can('finance:read')" label="收支明细" lazy
        ><PaymentLedger :tenant-id="tenantId"
      /></el-tab-pane>
      <el-tab-pane v-if="can('quota:read')" label="功能与额度">
        <el-alert
          v-if="!isHeadquarters"
          title="额度与功能由总部统一配置。如需调整，请联系总部。"
          type="info"
          :closable="false"
        />
        <el-form label-position="top" class="form">
          <el-form-item v-if="isHeadquarters" label="使用套餐">
            <el-select v-model="planId" clearable placeholder="单独配置此租户" @change="selectPlan">
              <el-option
                v-for="plan in plans"
                :key="plan.id"
                :value="plan.id"
                :label="plan.name"
                :disabled="!plan.enabled"
              />
            </el-select>
          </el-form-item>
          <p class="muted"
            >资源上限填 -1
            表示不限；订单和短信按自然月统计，其余为累计资源数。套餐调整不会自动改动已分配租户，需重新保存分配。</p
          >
          <div class="quota-grid">
            <el-form-item
              v-for="resource in resources"
              :key="resource.key"
              :label="resource.label + ' · 已使用 ' + (usage[resource.key] || 0)"
            >
              <el-input-number
                v-model="limits[resource.key]"
                :min="-1"
                :max="100000000"
                :disabled="!isHeadquarters || !!planId"
              />
            </el-form-item>
          </div>
          <el-form-item label="开通功能"
            ><el-checkbox-group v-model="features" :disabled="!isHeadquarters || !!planId"
              ><el-checkbox
                v-for="feature in featureOptions"
                :key="feature.key"
                :value="feature.key"
                >{{ feature.label }}</el-checkbox
              ></el-checkbox-group
            ></el-form-item
          >
          <el-space v-if="isHeadquarters"
            ><el-button type="primary" @click="saveQuota">保存租户额度</el-button
            ><el-button @click="createPlan">将当前配置另存为套餐</el-button></el-space
          >
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="分佣规则">
        <el-form label-position="top" class="form">
          <el-alert
            title="按订单完成时的规则生成分佣快照；后续规则调整只影响新的完工订单。退款产生反向明细，不覆盖已打款记录。"
            type="info"
            :closable="false"
          />
          <div class="quota-grid rule-fields">
            <el-form-item label="平台抽成（%）"
              ><el-input-number
                v-model="platformPercent"
                :min="0"
                :max="100"
                :precision="2"
                :disabled="!isHeadquarters"
            /></el-form-item>
            <el-form-item label="服务人员分成（%）"
              ><el-input-number
                v-model="workerPercent"
                :min="0"
                :max="100"
                :precision="2"
                :disabled="!isHeadquarters"
            /></el-form-item>
            <el-form-item label="建议结算周期（天）"
              ><el-input-number
                v-model="rule.cycleDays"
                :min="1"
                :max="90"
                :disabled="!isHeadquarters"
            /></el-form-item>
          </div>
          <el-form-item label="启用分佣"
            ><el-switch v-model="rule.enabled" :disabled="!isHeadquarters"
          /></el-form-item>
          <el-button v-if="isHeadquarters" type="primary" @click="saveRule">保存分佣规则</el-button>
        </el-form>
      </el-tab-pane>
      <el-tab-pane v-if="can('finance:read')" label="结算单">
        <el-form inline class="statement-form">
          <el-form-item label="结算对象"
            ><el-select v-model="statement.beneficiary" style="width: 140px"
              ><el-option label="门店" value="STORE" /><el-option
                label="服务人员"
                value="WORKER" /></el-select
          ></el-form-item>
          <el-form-item label="对象编号"
            ><el-input-number v-model="statement.beneficiaryId" :min="1"
          /></el-form-item>
          <el-form-item label="账期"
            ><el-date-picker
              v-model="period"
              type="daterange"
              value-format="YYYY-MM-DD"
              :clearable="false"
          /></el-form-item>
          <el-form-item
            ><el-button v-if="can('finance:statement')" type="primary" @click="generate"
              >生成结算单</el-button
            ><el-button @click="loadStatements">刷新</el-button></el-form-item
          >
        </el-form>
        <el-table :data="statements" empty-text="当前租户暂无结算单">
          <el-table-column prop="id" label="编号" width="90" />
          <el-table-column label="对象" min-width="130"
            ><template #default="{ row }"
              >{{ row.beneficiary === 'STORE' ? '门店' : '服务人员' }} #{{
                row.beneficiary_id
              }}</template
            ></el-table-column
          >
          <el-table-column label="账期" min-width="210"
            ><template #default="{ row }"
              >{{ row.period_start }} 至 {{ row.period_end }}</template
            ></el-table-column
          >
          <el-table-column label="金额" min-width="120"
            ><template #default="{ row }"
              >¥{{ (Number(row.amount_cents) / 100).toFixed(2) }}</template
            ></el-table-column
          >
          <el-table-column label="状态" width="110"
            ><template #default="{ row }">{{
              statusNames[row.status] || row.status
            }}</template></el-table-column
          >
          <el-table-column prop="payment_reference" label="打款凭证" min-width="160" />
          <el-table-column label="操作" min-width="200"
            ><template #default="{ row }">
              <el-button
                v-if="row.status === 'DRAFT' && can('finance:approve')"
                text
                type="primary"
                @click="action(row.id, 'approve')"
                >审核</el-button
              >
              <el-button
                v-if="row.status === 'APPROVED' && can('finance:payout')"
                text
                type="primary"
                @click="paid(row.id)"
                >登记打款</el-button
              >
              <el-button
                v-if="row.status === 'PAID' && can('finance:reconcile')"
                text
                type="primary"
                @click="action(row.id, 'reconcile')"
                >完成对账</el-button
              >
            </template></el-table-column
          >
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </HmPage>
</template>
<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTenantId } from '@/utils/auth'
import * as api from '@/api/homemaking'
import HmPage from './components/HmPage.vue'
import PaymentLedger from './components/PaymentLedger.vue'
import { useHmAccess } from './useAccess'
const { can, loadAccess } = useHmAccess()

defineOptions({ name: 'HomemakingFinance' })
const isHeadquarters = computed(() => can('platform:manage'))
const tenantId = ref(Number(getTenantId()) || 1),
  loading = ref(false),
  quotaVersion = ref(0),
  planId = ref<number>()
const limits = ref<Record<string, number>>({}),
  features = ref<string[]>([]),
  usage = ref<Record<string, number>>({}),
  plans = ref<any[]>([]),
  statements = ref<any[]>([])
const rule = ref({ platformBps: 0, workerBps: 0, cycleDays: 30, enabled: false, version: 0 })
const resources = [
  { key: 'stores', label: '门店' },
  { key: 'workers', label: '服务人员' },
  { key: 'services', label: '服务项目' },
  { key: 'customers', label: '客户' },
  { key: 'orders', label: '本月订单' },
  { key: 'sms', label: '本月短信' }
]
const featureOptions = [
  { key: 'portal', label: '独立官网' },
  { key: 'domain', label: '自定义域名' },
  { key: 'mini', label: '小程序' },
  { key: 'mp', label: '公众号' },
  { key: 'sms', label: '短信通知' },
  { key: 'worker', label: '服务人员工作台' },
  { key: 'settlement', label: '分佣结算' }
]
const statusNames: Record<string, string> = {
  DRAFT: '待审核',
  APPROVED: '待打款',
  PAID: '已登记打款',
  RECONCILED: '已对账'
}
const platformPercent = computed({
  get: () => rule.value.platformBps / 100,
  set: (value: number) => {
    rule.value.platformBps = Math.round(value * 100)
  }
})
const workerPercent = computed({
  get: () => rule.value.workerBps / 100,
  set: (value: number) => {
    rule.value.workerBps = Math.round(value * 100)
  }
})
const statement = ref({ beneficiary: 'STORE', beneficiaryId: 1 })
const period = ref([dayjs().startOf('month').format('YYYY-MM-DD'), dayjs().format('YYYY-MM-DD')])
async function load() {
  loading.value = true
  try {
    if (can('quota:read')) {
      const q = await api.getQuota(tenantId.value)
      quotaVersion.value = Number(q.version || 0)
      planId.value = q.planId || undefined
      limits.value = Object.fromEntries(resources.map((r) => [r.key, q.limits?.[r.key] ?? -1]))
      features.value = q.features || []
      usage.value = q.usage || {}
    }
    const r = await api.getCommissionRule(tenantId.value)
    rule.value = {
      platformBps: Number(r.platform_bps),
      workerBps: Number(r.worker_bps),
      cycleDays: Number(r.cycle_days),
      enabled: !!r.enabled,
      version: Number(r.version)
    }
    if (isHeadquarters.value) {
      plans.value = await api.listPlans()
      await loadStatements()
    }
  } finally {
    loading.value = false
  }
}
function selectPlan(id?: number) {
  const plan = plans.value.find((p) => p.id === id)
  if (plan) {
    const saved = JSON.parse(plan.limits_json)
    limits.value = Object.fromEntries(resources.map((r) => [r.key, saved[r.key] ?? -1]))
    features.value = JSON.parse(plan.features_json)
  }
}
async function saveQuota() {
  await api.saveQuota(tenantId.value, {
    planId: planId.value || null,
    limits: limits.value,
    features: features.value,
    version: quotaVersion.value
  })
  ElMessage.success('租户额度已保存')
  await load()
}
async function createPlan() {
  const { value } = await ElMessageBox.prompt('为当前额度与功能组合命名', '另存为套餐', {
    inputValidator: (v) => !!v?.trim() || '请填写套餐名称'
  })
  await api.savePlan({
    name: value.trim(),
    limits: limits.value,
    features: features.value,
    enabled: true,
    version: 0
  })
  plans.value = await api.listPlans()
  ElMessage.success('套餐已保存，可选择后分配给租户')
}
async function saveRule() {
  if (rule.value.platformBps + rule.value.workerBps > 10000)
    return ElMessage.warning('两项分成合计不能超过 100%')
  await api.saveCommissionRule(tenantId.value, rule.value)
  ElMessage.success('分佣规则已保存')
  await load()
}
async function loadStatements() {
  statements.value = await api.listStatements(tenantId.value)
}
async function generate() {
  await api.createStatement({
    ...statement.value,
    tenantId: tenantId.value,
    periodStart: period.value[0],
    periodEnd: period.value[1]
  })
  ElMessage.success('结算单已生成')
  await loadStatements()
}
async function action(id: number, a: string, data = {}) {
  await api.statementAction(id, a, data)
  ElMessage.success('结算单已更新')
  await loadStatements()
}
async function paid(id: number) {
  const { value } = await ElMessageBox.prompt('填写已实际打款的银行流水号或凭证编号', '登记打款', {
    inputValidator: (v) => !!v?.trim() || '请填写凭证编号'
  })
  await action(id, 'paid', { reference: value.trim() })
}
onMounted(async () => {
  await loadAccess()
  await load()
})
</script>
<style scoped>
.tenant-picker {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}
.form {
  max-width: 900px;
}
.quota-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 24px;
}
.muted {
  color: var(--el-text-color-secondary);
  line-height: 1.8;
}
.rule-fields {
  margin-top: 24px;
}
.statement-form {
  margin: 20px 0;
}
@media (max-width: 700px) {
  .quota-grid {
    grid-template-columns: 1fr;
  }
  .tenant-picker {
    flex-wrap: wrap;
  }
}
</style>
