<template>
  <HmPage
    eyebrow="HM · 平台合作网络"
    title="加盟商与合同"
    description="平台统一维护加盟档案、区域、合同和保证金。关联租户仍按普通租户隔离经营数据。"
  >
    <template #actions>
      <el-button v-if="can('franchise:write')" type="primary" @click="openProfile()"
        >新建加盟商</el-button
      >
    </template>
    <div class="overview">
      <div><strong>{{ rows.length }}</strong><span>加盟档案</span></div>
      <div><strong>{{ activeCount }}</strong><span>合作中</span></div>
      <div><strong>{{ contractCount }}</strong><span>生效合同</span></div>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" empty-text="尚未建立加盟商档案" row-key="id">
      <el-table-column label="加盟商" min-width="220">
        <template #default="{ row }">
          <button class="company-link" @click="openDetail(row.id)">{{ row.company_name }}</button>
          <small>{{ row.code }} · 租户 {{ row.tenant_name }}</small>
        </template>
      </el-table-column>
      <el-table-column label="负责人" min-width="150">
        <template #default="{ row }">{{ row.leader_name }}<small>{{ row.leader_mobile_masked }}</small></template>
      </el-table-column>
      <el-table-column label="合同" width="110">
        <template #default="{ row }">{{ row.active_contracts ? '生效中' : '无生效合同' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="openDetail(row.id)">查看</el-button>
          <el-button v-if="can('franchise:write') && row.status !== 'TERMINATED'" text @click="editProfile(row.id)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="detailVisible" size="min(760px, 96vw)" :with-header="false">
      <div v-if="detail" class="detail-shell">
        <header class="detail-head">
          <div><p>{{ detail.code }}</p><h2>{{ detail.company_name }}</h2><span>{{ detail.tenant_name }} · {{ statusName(detail.status) }}</span></div>
          <el-button circle @click="detailVisible = false"><Icon icon="ep:close" /></el-button>
        </header>
        <el-descriptions :column="descriptionColumns" border>
          <el-descriptions-item label="负责人">{{ detail.leader_name }} · {{ detail.leader_mobile_masked }}</el-descriptions-item>
          <el-descriptions-item label="加盟区域">{{ detail.regions?.map((r) => r.region_name).join('、') }}</el-descriptions-item>
          <el-descriptions-item label="统一信用代码">{{ detail.credit_code || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ detail.contact_email || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="注册地址" :span="descriptionColumns">{{ detail.registered_address || '未填写' }}</el-descriptions-item>
          <el-descriptions-item label="结算账户">{{ detail.settlement_account_name }} · {{ detail.settlement_account_masked }}</el-descriptions-item>
          <el-descriptions-item label="开户银行">{{ detail.settlement_bank_name }}</el-descriptions-item>
        </el-descriptions>
        <div class="detail-actions">
          <el-button v-if="can('franchise:settlement')" @click="revealSensitive">核对敏感资料</el-button>
          <el-button v-if="can('franchise:contract') && detail.status !== 'TERMINATED'" type="primary" @click="openContract()">新建合同</el-button>
          <el-button v-if="can('franchise:write') && detail.status !== 'TERMINATED'" type="danger" plain @click="terminateProfile">终止合作</el-button>
        </div>
        <section class="section">
          <div class="section-title"><div><p>CONTRACTS</p><h3>合同与保证金</h3></div></div>
          <el-empty v-if="!detail.contracts?.length" description="暂无合同" />
          <article v-for="contract in detail.contracts" :key="contract.id" class="contract-card">
            <div class="contract-main">
              <div><strong>{{ contract.contract_no }}</strong><span>{{ contract.start_date }} 至 {{ contract.end_date }}</span></div>
              <div class="contract-tags"><el-tag :type="statusType(contract.status)">{{ statusName(contract.status) }}</el-tag><el-tag effect="plain">保证金 {{ depositName(contract.deposit_status) }}</el-tag></div>
            </div>
            <div class="money"><span>约定保证金</span><strong>¥{{ money(contract.deposit_cents) }}</strong></div>
            <div v-if="can('franchise:contract') || can('franchise:settlement')" class="contract-actions">
              <el-button v-if="contract.status === 'DRAFT' && can('franchise:contract')" text @click="openContract(contract)">编辑</el-button>
              <el-button v-if="contract.status === 'DRAFT' && can('franchise:contract')" text type="primary" @click="activate(contract)">生效</el-button>
              <el-button v-if="['ACTIVE','EXPIRED','TERMINATED'].includes(contract.status) && can('franchise:contract')" text @click="openRenewal(contract)">续约</el-button>
              <el-button v-if="contract.status === 'ACTIVE' && can('franchise:contract')" text type="danger" @click="terminateContract(contract)">提前终止</el-button>
              <el-button v-if="can('franchise:settlement') && canDeposit(contract)" text @click="openDeposit(contract)">登记保证金</el-button>
            </div>
          </article>
        </section>
        <section class="section">
          <div class="section-title"><div><p>LEDGER</p><h3>保证金流水</h3></div></div>
          <el-table :data="detail.depositEntries || []" empty-text="暂无保证金流水" size="small">
            <el-table-column prop="occurred_at" label="发生时间" min-width="160" />
            <el-table-column label="类型" width="90"><template #default="{ row }">{{ depositEntryName(row.entry_type) }}</template></el-table-column>
            <el-table-column label="金额" width="110"><template #default="{ row }">¥{{ money(row.amount_cents) }}</template></el-table-column>
            <el-table-column prop="reference_no" label="凭证" min-width="130" />
            <el-table-column prop="note" label="备注" min-width="150" show-overflow-tooltip />
          </el-table>
        </section>
        <section class="section">
          <div class="section-title"><div><p>AUDIT</p><h3>操作记录</h3></div></div>
          <el-timeline>
            <el-timeline-item v-for="(log, index) in detail.auditLogs || []" :key="index" :timestamp="String(log.created_at)" placement="top">
              <strong>{{ auditName(log.action) }}</strong><p>{{ log.reason || '系统记录' }} · 操作人 {{ log.actor_id || '系统' }}</p>
            </el-timeline-item>
          </el-timeline>
        </section>
      </div>
    </el-drawer>

    <el-dialog v-model="profileVisible" :title="editingProfile ? '编辑加盟档案' : '新建加盟档案'" width="min(760px, 94vw)" :close-on-click-modal="false">
      <el-form label-position="top" class="form-grid">
        <el-form-item label="关联 SaaS 租户" required><el-select v-model="profile.tenantId" filterable :disabled="!!editingProfile"><el-option v-for="tenant in tenants" :key="tenant.id" :label="`${tenant.name}（${tenant.id}）`" :value="Number(tenant.id)" /></el-select></el-form-item>
        <el-form-item label="加盟商编号" required><el-input v-model="profile.code" maxlength="40" /></el-form-item>
        <el-form-item label="公司名称" required><el-input v-model="profile.companyName" maxlength="160" /></el-form-item>
        <el-form-item label="统一信用代码"><el-input v-model="profile.creditCode" maxlength="32" /></el-form-item>
        <el-form-item label="负责人" required><el-input v-model="profile.leaderName" maxlength="80" /></el-form-item>
        <el-form-item :label="editingProfile ? '负责人手机号（留空保持不变）' : '负责人手机号'" required><el-input v-model="profile.leaderMobile" maxlength="32" /></el-form-item>
        <el-form-item label="联系邮箱"><el-input v-model="profile.contactEmail" maxlength="160" /></el-form-item>
        <el-form-item label="注册地址"><el-input v-model="profile.registeredAddress" maxlength="500" /></el-form-item>
        <el-form-item label="结算账户名称" required><el-input v-model="profile.settlementAccountName" maxlength="160" /></el-form-item>
        <el-form-item label="开户银行" required><el-input v-model="profile.settlementBankName" maxlength="160" /></el-form-item>
        <el-form-item :label="editingProfile ? '结算账号（留空保持不变）' : '结算账号'" required><el-input v-model="profile.settlementAccount" maxlength="100" /></el-form-item>
        <el-form-item label="加盟区域" required class="span-all">
          <div class="regions"><div v-for="(region, index) in profile.regions" :key="index"><el-input v-model="region.code" placeholder="行政区代码" maxlength="32" /><el-input v-model="region.name" placeholder="区域名称" maxlength="100" /><el-button text type="danger" @click="profile.regions.splice(index, 1)">删除</el-button></div><el-button plain @click="profile.regions.push({ code: '', name: '' })">添加区域</el-button></div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="profileVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="contractVisible" :title="renewingContract ? '创建续约合同' : editingContract ? '编辑合同草稿' : '新建合同'" width="min(660px, 94vw)" :close-on-click-modal="false">
      <el-form label-position="top" class="form-grid">
        <el-form-item label="合同编号" required><el-input v-model="contractForm.contractNo" maxlength="60" /></el-form-item>
        <el-form-item label="签署日期" required><el-date-picker v-model="contractForm.signedOn" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="合同开始" required><el-date-picker v-model="contractForm.startDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="合同结束" required><el-date-picker v-model="contractForm.endDate" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="保证金（元）"><el-input-number v-model="depositYuan" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="contractForm.remark" maxlength="1000" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="contractVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveContract">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="depositVisible" title="登记保证金流水" width="min(540px, 94vw)" :close-on-click-modal="false">
      <el-alert title="请先完成真实收款、退款或扣罚，再登记凭证。流水保存后不可修改。" type="warning" :closable="false" />
      <el-form label-position="top" class="deposit-form">
        <el-form-item label="类型" required><el-radio-group v-model="depositForm.type"><el-radio-button v-if="selectedContract?.status === 'DRAFT' || selectedContract?.status === 'ACTIVE'" value="RECEIPT">收取</el-radio-button><el-radio-button v-if="selectedContract?.status === 'EXPIRED' || selectedContract?.status === 'TERMINATED'" value="REFUND">退还</el-radio-button><el-radio-button v-if="selectedContract?.status === 'EXPIRED' || selectedContract?.status === 'TERMINATED'" value="FORFEIT">扣罚</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="金额（元）" required><el-input-number v-model="depositAmountYuan" :min="0.01" :precision="2" /></el-form-item>
        <el-form-item label="渠道" required><el-select v-model="depositForm.channel"><el-option v-for="item in channels" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
        <el-form-item label="实际发生时间" required><el-date-picker v-model="depositForm.occurredAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
        <el-form-item label="银行流水或凭证号"><el-input v-model="depositForm.referenceNo" maxlength="100" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="depositForm.note" type="textarea" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="depositVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveDeposit">确认登记</el-button></template>
    </el-dialog>
  </HmPage>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/homemaking'
import HmPage from './components/HmPage.vue'
import { useHmAccess } from './useAccess'

defineOptions({ name: 'HomemakingFranchise' })
const { can, loadAccess } = useHmAccess()
const rows = ref<any[]>([]), tenants = ref<any[]>([]), detail = ref<any>()
const loading = ref(false), saving = ref(false), detailVisible = ref(false), profileVisible = ref(false), contractVisible = ref(false), depositVisible = ref(false)
const editingProfile = ref<number>(), editingContract = ref<any>(), renewingContract = ref<any>(), selectedContract = ref<any>()
const activeCount = computed(() => rows.value.filter((row) => row.status === 'ACTIVE').length)
const contractCount = computed(() => rows.value.reduce((sum, row) => sum + Number(row.active_contracts || 0), 0))
const viewportWidth = ref(window.innerWidth)
const descriptionColumns = computed(() => viewportWidth.value < 680 ? 1 : 2)
const emptyProfile = () => ({ tenantId: undefined as number | undefined, code: '', companyName: '', creditCode: '', leaderName: '', leaderMobile: '', contactEmail: '', registeredAddress: '', settlementAccountName: '', settlementBankName: '', settlementAccount: '', regions: [{ code: '', name: '' }], version: 0 })
const profile = ref(emptyProfile())
const emptyContract = () => ({ contractNo: '', signedOn: dayjs().format('YYYY-MM-DD'), startDate: dayjs().format('YYYY-MM-DD'), endDate: dayjs().add(1, 'year').subtract(1, 'day').format('YYYY-MM-DD'), depositCents: 0, remark: '', version: 0 })
const contractForm = ref(emptyContract())
const depositYuan = computed({ get: () => contractForm.value.depositCents / 100, set: (value: number) => { contractForm.value.depositCents = Math.round(Number(value || 0) * 100) } })
const depositForm = ref({ type: 'RECEIPT', amountCents: 0, channel: 'BANK_TRANSFER', occurredAt: '', referenceNo: '', requestKey: '', note: '' })
const depositAmountYuan = computed({ get: () => depositForm.value.amountCents / 100, set: (value: number) => { depositForm.value.amountCents = Math.round(Number(value || 0) * 100) } })
const channels = [{ value: 'CASH', label: '现金' }, { value: 'WECHAT_TRANSFER', label: '微信转账' }, { value: 'ALIPAY_TRANSFER', label: '支付宝转账' }, { value: 'BANK_TRANSFER', label: '银行转账' }, { value: 'OTHER', label: '其他' }]
const statuses: Record<string, string> = { PENDING: '待签约', ACTIVE: '合作中', SUSPENDED: '已暂停', TERMINATED: '已终止', DRAFT: '草稿', EXPIRED: '已到期' }
const deposits: Record<string, string> = { NOT_REQUIRED: '无需缴纳', PENDING: '待收齐', PAID: '已收齐', REFUNDED: '已退还', FORFEITED: '已扣罚' }
const audits: Record<string, string> = { PROFILE_CREATED: '建立加盟档案', PROFILE_UPDATED: '更新加盟档案', SENSITIVE_VIEWED: '查看敏感资料', CONTRACT_CREATED: '新建合同', CONTRACT_UPDATED: '更新合同', CONTRACT_ACTIVATED: '合同生效', CONTRACT_RENEWED: '创建续约合同', CONTRACT_TERMINATED: '合同提前终止', CONTRACT_EXPIRED: '合同到期', DEPOSIT_RECEIPT: '登记保证金收款', DEPOSIT_REFUND: '登记保证金退还', DEPOSIT_FORFEIT: '登记保证金扣罚', FRANCHISE_TERMINATED: '终止加盟合作' }
const statusName = (value: string) => statuses[value] || value
const depositName = (value: string) => deposits[value] || value
const auditName = (value: string) => audits[value] || value
const depositEntryName = (value: string) => ({ RECEIPT: '收取', REFUND: '退还', FORFEIT: '扣罚' }[value] || value)
const statusType = (value: string) => value === 'ACTIVE' ? 'success' : value === 'DRAFT' || value === 'PENDING' ? 'info' : value === 'TERMINATED' ? 'danger' : 'warning'
const money = (cents: number) => (Number(cents || 0) / 100).toFixed(2)
async function load() { loading.value = true; try { rows.value = await api.listFranchises() } finally { loading.value = false } }
async function refreshDetail() { if (detail.value?.id) detail.value = await api.getFranchise(detail.value.id) }
async function openDetail(id: number) { detail.value = await api.getFranchise(id); detailVisible.value = true }
async function openProfile() { editingProfile.value = undefined; profile.value = emptyProfile(); tenants.value = await api.listFranchiseTenants(); profileVisible.value = true }
async function editProfile(id: number) { const row = await api.getFranchise(id); editingProfile.value = id; tenants.value = await api.listFranchiseTenants(); profile.value = { tenantId: Number(row.tenant_id), code: row.code, companyName: row.company_name, creditCode: row.credit_code || '', leaderName: row.leader_name, leaderMobile: '', contactEmail: row.contact_email || '', registeredAddress: row.registered_address || '', settlementAccountName: row.settlement_account_name, settlementBankName: row.settlement_bank_name, settlementAccount: '', regions: row.regions.map((r: any) => ({ code: r.region_code, name: r.region_name })), version: Number(row.version) }; profileVisible.value = true }
function profileValid() { return profile.value.tenantId && profile.value.code.trim() && profile.value.companyName.trim() && profile.value.leaderName.trim() && profile.value.settlementAccountName.trim() && profile.value.settlementBankName.trim() && (editingProfile.value || profile.value.leaderMobile.trim()) && (editingProfile.value || profile.value.settlementAccount.trim()) && profile.value.regions.length && profile.value.regions.every((r) => r.code.trim() && r.name.trim()) }
async function saveProfile() { if (!profileValid()) return ElMessage.warning('请完整填写必填项和加盟区域'); saving.value = true; try { if (editingProfile.value) await api.updateFranchise(editingProfile.value, profile.value); else await api.createFranchise(profile.value); profileVisible.value = false; ElMessage.success('加盟档案已保存'); await load(); if (detailVisible.value && editingProfile.value) await openDetail(editingProfile.value) } finally { saving.value = false } }
function openContract(row?: any) { renewingContract.value = undefined; editingContract.value = row; contractForm.value = row ? { contractNo: row.contract_no, signedOn: row.signed_on, startDate: row.start_date, endDate: row.end_date, depositCents: Number(row.deposit_cents), remark: row.remark || '', version: Number(row.version) } : emptyContract(); contractVisible.value = true }
function openRenewal(row: any) { editingContract.value = undefined; renewingContract.value = row; const start = dayjs(row.end_date).isAfter(dayjs()) ? dayjs(row.end_date).add(1, 'day') : dayjs(); contractForm.value = { ...emptyContract(), contractNo: `${row.contract_no}-R`, startDate: start.format('YYYY-MM-DD'), endDate: start.add(1, 'year').subtract(1, 'day').format('YYYY-MM-DD') }; contractVisible.value = true }
async function saveContract() { if (!contractForm.value.contractNo.trim() || !contractForm.value.signedOn || !contractForm.value.startDate || !contractForm.value.endDate) return ElMessage.warning('请完整填写合同日期和编号'); saving.value = true; try { if (renewingContract.value) await api.renewFranchiseContract(detail.value.id, renewingContract.value.id, contractForm.value); else if (editingContract.value) await api.updateFranchiseContract(detail.value.id, editingContract.value.id, contractForm.value); else await api.createFranchiseContract(detail.value.id, contractForm.value); contractVisible.value = false; ElMessage.success('合同草稿已保存'); await refreshDetail(); await load() } finally { saving.value = false } }
async function activate(row: any) { await ElMessageBox.confirm(row.deposit_cents ? '确认保证金已经足额登记，并让合同正式生效？' : '确认让合同正式生效？', '合同生效'); await api.activateFranchiseContract(detail.value.id, row.id, Number(row.version)); ElMessage.success('合同已生效'); await refreshDetail(); await load() }
async function terminateContract(row: any) { const { value } = await ElMessageBox.prompt('提前终止会保留合同和保证金流水，请填写双方确认的原因。', '提前终止合同', { inputValidator: (v) => !!v?.trim() || '请填写终止原因' }); await api.terminateFranchiseContract(detail.value.id, row.id, { reason: value.trim(), version: Number(row.version) }); ElMessage.success('合同已终止，请继续处理保证金余额'); await refreshDetail(); await load() }
function canDeposit(row: any) { return ['DRAFT','ACTIVE','EXPIRED','TERMINATED'].includes(row.status) && row.deposit_cents > 0 && !['REFUNDED','FORFEITED'].includes(row.deposit_status) }
function openDeposit(row: any) { selectedContract.value = row; const disposal = ['EXPIRED','TERMINATED'].includes(row.status); depositForm.value = { type: disposal ? 'REFUND' : 'RECEIPT', amountCents: Number(row.deposit_cents), channel: 'BANK_TRANSFER', occurredAt: dayjs().format('YYYY-MM-DDTHH:mm:ss'), referenceNo: '', requestKey: globalThis.crypto?.randomUUID?.() || `${Date.now()}-${row.id}`, note: '' }; depositVisible.value = true }
async function saveDeposit() { if (!selectedContract.value || depositForm.value.amountCents < 1 || !depositForm.value.occurredAt) return ElMessage.warning('请填写实际金额与发生时间'); await ElMessageBox.confirm('确认真实款项已经发生，并将这条流水永久记入账簿？', '确认保证金流水'); saving.value = true; try { await api.recordFranchiseDeposit(detail.value.id, selectedContract.value.id, depositForm.value); depositVisible.value = false; ElMessage.success('保证金流水已登记'); await refreshDetail() } finally { saving.value = false } }
async function revealSensitive() { const { value } = await ElMessageBox.prompt('敏感资料查看会记录操作人、时间与用途。', '填写查看用途', { inputValidator: (v) => !!v?.trim() || '请填写查看用途' }); const data = await api.revealFranchiseSensitive(detail.value.id, value.trim()); await ElMessageBox.alert(`负责人手机号：${data.leaderMobile}\n结算账号：${data.settlementAccount}`, '敏感资料', { confirmButtonText: '已核对' }); await refreshDetail() }
async function terminateProfile() { const { value } = await ElMessageBox.prompt('终止前必须结束全部生效合同并结清保证金。历史业务和租户数据不会删除。', '终止加盟合作', { inputValidator: (v) => !!v?.trim() || '请填写终止原因' }); await api.terminateFranchise(detail.value.id, { reason: value.trim(), version: Number(detail.value.version) }); ElMessage.success('加盟合作已终止'); await refreshDetail(); await load() }
function syncViewport() { viewportWidth.value = window.innerWidth }
onMounted(async () => { window.addEventListener('resize', syncViewport); await loadAccess(); await load() })
onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))
</script>

<style scoped>
.overview { display: grid; grid-template-columns: repeat(3, minmax(120px, 1fr)) auto; gap: 12px; align-items: stretch; margin-bottom: 22px; }
.overview > div { min-height: 82px; padding: 16px 18px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; background: linear-gradient(145deg, var(--el-fill-color-blank), var(--el-fill-color-light)); }
.overview strong { display: block; font-size: 25px; font-variant-numeric: tabular-nums; }.overview span, small { display: block; margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.company-link { padding: 0; border: 0; background: transparent; color: var(--el-color-primary); font: inherit; font-weight: 600; cursor: pointer; text-align: left; }
.detail-shell { padding: 8px 6px 30px; }.detail-head { display: flex; justify-content: space-between; gap: 16px; padding: 14px 0 26px; }.detail-head p,.section-title p { margin: 0 0 6px; color: var(--el-color-primary); font-size: 11px; font-weight: 700; letter-spacing: 1.8px; }.detail-head h2,.section-title h3 { margin: 0; }.detail-head span { display: block; margin-top: 8px; color: var(--el-text-color-secondary); }.detail-actions { display: flex; flex-wrap: wrap; gap: 10px; margin: 20px 0 32px; }
.section { margin-top: 30px; }.section-title { margin-bottom: 14px; }.contract-card { margin-bottom: 12px; padding: 18px; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color); }.contract-main { display: flex; justify-content: space-between; gap: 16px; }.contract-main strong,.contract-main span { display: block; }.contract-main span { margin-top: 7px; color: var(--el-text-color-secondary); font-size: 13px; }.contract-tags { display: flex; flex-wrap: wrap; gap: 6px; justify-content: flex-end; }.money { display: flex; justify-content: space-between; margin-top: 16px; padding-top: 14px; border-top: 1px dashed var(--el-border-color); }.money span { color: var(--el-text-color-secondary); }.contract-actions { display: flex; flex-wrap: wrap; gap: 4px; margin: 8px -12px -8px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }.span-all { grid-column: 1 / -1; }.regions { width: 100%; }.regions > div { display: grid; grid-template-columns: minmax(120px,.7fr) minmax(150px,1fr) auto; gap: 8px; margin-bottom: 8px; }.deposit-form { margin-top: 20px; }.el-select,.el-date-editor { width: 100%; }.el-timeline p { margin: 5px 0 0; color: var(--el-text-color-secondary); overflow-wrap: anywhere; }
@media (max-width: 760px) { .overview { grid-template-columns: repeat(3, 1fr); }.overview > .el-button { grid-column: 1 / -1; }.form-grid { grid-template-columns: 1fr; }.span-all { grid-column: auto; }.contract-main { display: block; }.contract-tags { justify-content: flex-start; margin-top: 12px; } }
@media (max-width: 480px) { .overview { grid-template-columns: 1fr; }.overview > .el-button { grid-column: auto; }.overview > div { min-height: auto; }.regions > div { grid-template-columns: 1fr; padding-bottom: 12px; border-bottom: 1px solid var(--el-border-color-lighter); } }
</style>
