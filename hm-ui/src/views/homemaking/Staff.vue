<template>
  <HmPage
    eyebrow="HM · 人员权限"
    title="各司其职，范围清晰。"
    description="为已有后台账号分配家政角色与门店。修改后，后端会立即按新的范围校验请求。"
  >
    <el-table :data="users" v-loading="loading" empty-text="请先在系统用户管理创建后台账号">
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="nickname" label="姓名" />
      <el-table-column label="家政角色"
        ><template #default="{ row }">{{ row.super_admin ? '超级管理员（保留）' : roleName(row.template_code) }}</template></el-table-column
      >
      <el-table-column label="操作"
        ><template #default="{ row }"
          ><el-button v-if="can('staff:write') && row.editable" link type="primary" @click="edit(row)"
            >分配角色与范围</el-button
          ></template
        ></el-table-column
      >
    </el-table>
    <el-dialog v-model="visible" title="分配家政权限" width="min(600px, 94vw)">
      <el-form label-position="top">
        <el-form-item label="后台账号"
          ><el-input :model-value="selected?.username" disabled
        /></el-form-item>
        <el-form-item label="角色模板"
          ><el-select v-model="templateCode" @change="storeIds = []"
            ><el-option
              v-for="t in templates"
              :key="t.code"
              :value="t.code"
              :label="t.name" /></el-select
        ></el-form-item>
        <el-form-item v-if="template?.range === 'STORES'" label="可操作门店（至少一个）"
          ><el-select v-model="storeIds" multiple filterable
            ><el-option v-for="s in stores" :key="s.id" :value="s.id" :label="s.name" /></el-select
        ></el-form-item>
        <el-alert :closable="false" :title="rangeLabel" type="info" />
        <p class="permissions-label">包含以下操作权限</p>
        <div class="permission-list"
          ><el-tag v-for="p in template?.permissions" :key="p" size="small">{{
            permissionName(p)
          }}</el-tag></div
        >
      </el-form>
      <template #footer
        ><el-button @click="visible = false">取消</el-button
        ><el-button type="primary" :loading="saving" @click="save">保存权限</el-button></template
      >
    </el-dialog>
  </HmPage>
</template>
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/config/axios'
import { listCatalog } from '@/api/homemaking'
import HmPage from './components/HmPage.vue'
import { useHmAccess } from './useAccess'
defineOptions({ name: 'HomemakingStaff' })
const { can, loadAccess } = useHmAccess()
const users = ref<any[]>([]),
  templates = ref<any[]>([]),
  stores = ref<any[]>([]),
  selected = ref<any>()
const loading = ref(false),
  saving = ref(false),
  visible = ref(false),
  templateCode = ref('SUPPORT'),
  storeIds = ref<number[]>([])
const template = computed(() => templates.value.find((t) => t.code === templateCode.value))
const rangeLabel = computed(() =>
  template.value?.range === 'STORES'
    ? '只可查看和操作所选门店的数据。'
    : template.value?.range === 'SELF'
      ? '只可处理本人任务；保存后需在排班中心绑定对应服务人员。'
      : '覆盖本租户全部门店；平台管理员可经授权租户访问进入其他租户。'
)
const roleName = (code: string) =>
  templates.value.find((t) => t.code === code)?.name || '未分配'
const groups: Record<string, string> = {
  stores: '门店',
  workers: '人员',
  services: '服务',
  orders: '订单',
  customers: '客户',
  aftersales: '售后',
  reviews: '评价',
  schedule: '排班',
  finance: '财务',
  brand: '品牌',
  notification: '通知',
  portal: '官网',
  staff: '权限',
  quota: '配额',
  platform: '平台',
  worker: '工作台'
}
const actions: Record<string, string> = {
  read: '查看',
  write: '编辑',
  bind: '绑定账号',
  dispatch: '派单',
  reschedule: '改期',
  cancel: '取消',
  fulfill: '履约',
  reject: '驳回',
  refund: '退款',
  moderate: '审核',
  templates: '班次模板',
  statement: '生成结算',
  approve: '审核结算',
  payout: '登记打款',
  reconcile: '对账',
  publish: '发布',
  manage: '管理'
}
function permissionName(p: string) {
  const [, group, action] = p.split(':')
  return `${groups[group] || group} · ${actions[action] || action}`
}
async function load() {
  loading.value = true
  try {
    users.value = await request.get({ url: '/homemaking/access/users' })
  } finally {
    loading.value = false
  }
}
async function edit(user: any) {
  selected.value = user
  const binding = await request.get({ url: `/homemaking/access/users/${user.id}` })
  templateCode.value = binding.roles[0]?.template_code || 'SUPPORT'
  storeIds.value = binding.storeIds
  visible.value = true
}
async function save() {
  if (template.value?.range === 'STORES' && !storeIds.value.length)
    return ElMessage.warning('请选择至少一个门店')
  await ElMessageBox.confirm('确认替换该账号的家政角色和数据范围？', '保存权限')
  saving.value = true
  try {
    await request.put({
      url: '/homemaking/access/users',
      data: {
        userId: selected.value.id,
        templateCode: templateCode.value,
        storeIds: template.value?.range === 'STORES' ? storeIds.value : []
      }
    })
    visible.value = false
    ElMessage.success('权限已更新')
    await load()
  } finally {
    saving.value = false
  }
}
onMounted(async () => {
  await loadAccess()
  templates.value = await request.get({ url: '/homemaking/access/templates' })
  let page = 1
  do {
    const data = await listCatalog('stores', { page, size: 100 })
    stores.value.push(...data.list)
    if (stores.value.length >= data.total) break
    page++
  } while (page <= 100)
  await load()
})
</script>
<style scoped>
.permission-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 220px;
  overflow: auto;
}
.permissions-label {
  margin: 20px 0 12px;
  color: var(--el-text-color-secondary);
}
</style>
