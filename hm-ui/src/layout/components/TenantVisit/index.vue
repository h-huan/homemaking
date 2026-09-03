<template>
  <div class="flex items-center gap-8px">
    <el-tag :type="me.platform ? 'warning' : 'info'">{{
      me.platform ? '平台管理' : '业务租户'
    }}</el-tag>
    <el-select
      v-if="me.platform"
      :model-value="value"
      filterable
      aria-label="当前业务租户"
      placeholder="当前业务租户"
      class="!w-180px"
      @change="handleChange"
    >
      <el-option v-for="item in tenants" :key="item.id" :label="item.name" :value="item.id" />
    </el-select>
    <span v-else class="max-w-180px truncate" :title="me.businessTenantName">{{
      me.businessTenantName
    }}</span>
  </div>
</template>
<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import * as TenantApi from '@/api/system/tenant'
import request from '@/config/axios'
import { setVisitTenantId } from '@/utils/auth'
import { ElMessageBox } from 'element-plus'
const value = ref<number>(),
  tenants = ref<any[]>([]),
  me = ref<any>({ platform: false, businessTenantName: '' })
async function handleChange(id: number) {
  if (id === value.value) return
  const tenant = tenants.value.find((t) => t.id === id)
  await ElMessageBox.confirm(
    `切换后，业务查询与操作将使用「${tenant?.name}」的数据范围。未保存的内容将丢失。`,
    '切换业务租户'
  )
  setVisitTenantId(id === me.value.accountTenantId ? 0 : id)
  // Recreate page state so prior-tenant forms cannot be submitted after switching.
  window.location.reload()
}
onMounted(async () => {
  me.value = await request.get({ url: '/system/platform-access/me' })
  value.value = me.value.businessTenantId
  if (me.value.platform) tenants.value = await TenantApi.getTenantList()
})
</script>
