<template>
  <HmPage
    eyebrow="HM · 平台身份"
    title="平台授权，独立管理。"
    description="平台管理员可跨租户管理。总部直营与加盟租户的日常角色在「人员权限」中分配。"
  >
    <template #actions
      ><el-button type="primary" @click="openGrant">授权平台管理员</el-button></template
    >
    <el-alert
      title="账号所属租户只是登录归属，不决定平台身份。授权即时生效，撤销后已有会话也会失去平台权限。"
      type="info"
      :closable="false"
      class="mb-20px"
    />
    <el-tabs v-model="tab" @tab-change="tabChanged">
      <el-tab-pane label="平台管理员" name="operators">
        <el-table :data="operators" v-loading="loading" empty-text="暂无平台授权记录">
          <el-table-column prop="username" label="账号" min-width="130" /><el-table-column
            prop="nickname"
            label="姓名"
            min-width="100"
          />
          <el-table-column prop="tenant_name" label="登录所属租户" min-width="170" />
          <el-table-column label="授权状态" min-width="110"
            ><template #default="{ row }"
              ><el-tag :type="row.enabled ? 'success' : 'info'">{{
                row.enabled ? '已授权' : '已撤销'
              }}</el-tag></template
            ></el-table-column
          >
          <el-table-column
            prop="reason"
            label="最近变更原因"
            min-width="220"
            show-overflow-tooltip
          />
          <el-table-column label="操作" width="120"
            ><template #default="{ row }"
              ><el-button
                v-if="row.user_id !== currentUser && row.enabled"
                link
                type="danger"
                @click="revoke(row)"
                >撤销授权</el-button
              ><span v-else-if="row.user_id === currentUser">当前账号</span></template
            ></el-table-column
          >
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="授权与访问记录" name="logs">
        <el-table :data="logs" v-loading="loading" empty-text="暂无记录">
          <el-table-column label="时间" min-width="170"
            ><template #default="{ row }">{{
              formatDate(row.created_at)
            }}</template></el-table-column
          >
          <el-table-column label="操作" width="110"
            ><template #default="{ row }">{{
              actionNames[row.action] || row.action
            }}</template></el-table-column
          >
          <el-table-column prop="operator_id" label="操作人 ID" width="110" /><el-table-column
            prop="target_tenant_id"
            label="目标租户 ID"
            width="110"
          />
          <el-table-column prop="target_user_id" label="目标账号 ID" width="110" /><el-table-column
            prop="reason"
            label="原因"
            min-width="170"
            show-overflow-tooltip
          />
          <el-table-column
            prop="request_path"
            label="访问路径"
            min-width="240"
            show-overflow-tooltip
          />
        </el-table>
        <div class="mt-16px flex justify-end gap-12px"
          ><el-button :disabled="page === 1" @click="previousPage">上一页</el-button
          ><span class="self-center">第 {{ page }} 页</span
          ><el-button :disabled="logs.length < 30" @click="nextPage">下一页</el-button></div
        >
      </el-tab-pane>
    </el-tabs>
    <el-dialog
      v-model="visible"
      title="授权平台管理员"
      width="min(560px, 94vw)"
      :close-on-click-modal="false"
    >
      <el-form label-position="top">
        <el-form-item label="账号登录所属租户"
          ><el-select v-model="tenantId" filterable @change="resetCandidate"
            ><el-option
              v-for="tenant in tenants"
              :key="tenant.id"
              :label="tenant.name"
              :value="tenant.id" /></el-select
        ></el-form-item>
        <el-form-item label="已有后台账号"
          ><el-select
            v-model="userId"
            filterable
            remote
            :remote-method="search"
            :loading="searching"
            :disabled="!tenantId"
            placeholder="输入至少两个字查找账号或姓名"
            ><el-option
              v-for="user in candidates"
              :key="user.id"
              :value="user.id"
              :label="`${user.nickname} · ${user.username}`" /></el-select
        ></el-form-item>
        <el-form-item label="授权原因"
          ><el-input v-model="reason" type="textarea" maxlength="500" show-word-limit :rows="3"
        /></el-form-item>
        <el-alert
          type="warning"
          title="此授权允许管理所有租户及平台配置。请核对账号，不可通过普通员工角色授予。"
          :closable="false"
        />
      </el-form>
      <template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="saving"
          :disabled="!userId || !reason.trim()"
          @click="grant"
          >确认授权</el-button
        ></template
      >
    </el-dialog>
  </HmPage>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/config/axios'
import { getTenantList } from '@/api/system/tenant'
import { formatDate } from '@/utils/formatTime'
import HmPage from './components/HmPage.vue'
defineOptions({ name: 'HomemakingPlatformAccess' })
const operators = ref<any[]>([]),
  logs = ref<any[]>([]),
  tenants = ref<any[]>([]),
  candidates = ref<any[]>([])
const currentUser = ref(0),
  tenantId = ref<number>(),
  userId = ref<number>(),
  reason = ref(''),
  tab = ref('operators'),
  page = ref(1)
const visible = ref(false),
  loading = ref(false),
  saving = ref(false),
  searching = ref(false)
let searchGeneration = 0
const actionNames: Record<string, string> = {
  GRANT: '授予平台权限',
  REVOKE: '撤销平台权限',
  VISIT: '跨租户访问',
  MIGRATE: '迁移既有授权'
}
async function load() {
  loading.value = true
  try {
    if (tab.value === 'operators')
      operators.value = await request.get({ url: '/system/platform-access/operators' })
    else
      logs.value = await request.get({
        url: '/system/platform-access/logs',
        params: { page: page.value, size: 30 }
      })
  } finally {
    loading.value = false
  }
}
function resetCandidate() {
  searchGeneration++
  userId.value = undefined
  candidates.value = []
  searching.value = false
}
async function search(query: string) {
  const generation = ++searchGeneration
  if (!tenantId.value || query.trim().length < 2) {
    candidates.value = []
    searching.value = false
    return
  }
  searching.value = true
  try {
    const data = await request.get({
      url: '/system/platform-access/candidates',
      params: { tenantId: tenantId.value, query }
    })
    if (generation === searchGeneration)
      candidates.value = data.filter((u: any) => u.id !== currentUser.value)
  } finally {
    if (generation === searchGeneration) searching.value = false
  }
}
async function openGrant() {
  tab.value = 'operators'
  await load()
  tenants.value = await getTenantList()
  tenantId.value = undefined
  resetCandidate()
  reason.value = ''
  visible.value = true
}
async function grant() {
  if (!userId.value || !tenantId.value || !reason.value.trim()) return
  await ElMessageBox.confirm('确认授予该账号跨租户的平台管理权限？', '确认平台授权')
  saving.value = true
  try {
    const existing = operators.value.find((p) => p.user_id === userId.value)
    await request.put({
      url: '/system/platform-access/operators',
      data: {
        userId: userId.value,
        accountTenantId: tenantId.value,
        enabled: true,
        version: existing?.version || 0,
        reason: reason.value.trim()
      }
    })
    visible.value = false
    ElMessage.success('平台授权已生效')
    await load()
  } finally {
    saving.value = false
  }
}
async function revoke(row: any) {
  const { value } = await ElMessageBox.prompt(
    `撤销 ${row.username} 的平台权限，已有登录会话也会立即失效于平台操作。`,
    '撤销平台授权',
    {
      inputPlaceholder: '填写撤销原因',
      inputValidator: (v: string) => (!!v?.trim() && v.length <= 500) || '请填写 1～500 字的原因'
    }
  )
  await request.put({
    url: '/system/platform-access/operators',
    data: {
      userId: row.user_id,
      accountTenantId: row.account_tenant_id,
      enabled: false,
      version: row.version,
      reason: value.trim()
    }
  })
  ElMessage.success('平台权限已撤销')
  await load()
}
async function tabChanged() {
  page.value = 1
  await load()
}
async function previousPage() {
  page.value--
  await load()
}
async function nextPage() {
  page.value++
  await load()
}
onMounted(async () => {
  const me = await request.get({ url: '/system/platform-access/me' })
  currentUser.value = me.userId
  await load()
})
</script>
