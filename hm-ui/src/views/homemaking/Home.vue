<template>
  <div class="hm-workplace">
    <header
      ><img :src="tenantBrand.logo" alt="" /><div
        ><h1>{{ tenantBrand.name }}</h1
        ><p>服务、人员与订单，在这里有序协作。</p></div
      ></header
    >
    <el-alert v-if="error" :title="error" type="info" :closable="false" />
    <div v-loading="loading" class="hm-summary" v-hasPermi="['homemaking:manage']">
      <button
        v-for="module in visibleModules"
        :key="module.key"
        @click="router.push('/homemaking/operations')"
      >
        <span>{{ module.label }}</span
        ><strong>{{ totals[module.key] ?? '—' }}</strong
        ><small>查看与管理 →</small>
      </button>
    </div>
    <el-card shadow="never" v-hasPermi="['homemaking:manage']">
      <template #header
        ><div class="hm-heading"
          ><span>近期订单</span
          ><el-button text type="primary" @click="router.push('/homemaking/operations')"
            >全部订单</el-button
          ></div
        ></template
      >
      <el-table :data="recent" empty-text="还没有订单，先添加门店、人员和服务。">
        <el-table-column prop="id" label="订单号" width="100" /><el-table-column
          prop="service_name"
          label="服务"
        />
        <el-table-column label="金额"
          ><template #default="{ row }"
            >¥{{ (row.price_cents / 100).toFixed(2) }}</template
          ></el-table-column
        >
        <el-table-column label="状态"
          ><template #default="{ row }">{{
            states[row.status] || '待核对'
          }}</template></el-table-column
        >
      </el-table>
    </el-card>
  </div>
</template>
<script setup lang="ts">
import { tenantBrand, applyTenantBrand } from '@/hooks/web/useTenantBrand'
import { getBrand, listCatalog, listBusiness, type BusinessRow } from '@/api/homemaking'
import { checkPermi } from '@/utils/permission'
const router = useRouter()
const loading = ref(false),
  error = ref(''),
  recent = ref<BusinessRow[]>([])
const totals = reactive<Record<string, number>>({})
const modules = [
  { key: 'services', label: '服务项目' },
  { key: 'stores', label: '服务门店' },
  { key: 'workers', label: '服务人员' }
]
const visibleModules = computed(() =>
  modules.filter((item) => tenantBrand.homeModules.includes(item.key))
)
const states: Record<string, string> = {
  UNPAID: '待付款',
  PAID: '待派单',
  ASSIGNED: '待上门',
  IN_SERVICE: '服务中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDING: '退款中',
  REFUNDED: '已退款'
}
onMounted(async () => {
  if (!checkPermi(['homemaking:manage'])) return
  loading.value = true
  try {
    applyTenantBrand(await getBrand())
    await Promise.all(
      modules.map(async (item) => {
        totals[item.key] = (await listCatalog(item.key, { page: 1, size: 1 })).total
      })
    )
    recent.value = (await listBusiness('orders', { page: 1, size: 8 })).list
  } catch {
    error.value = '暂时无法加载业务数据，请稍后刷新。'
  } finally {
    loading.value = false
  }
})
</script>
<style scoped>
.hm-workplace {
  max-width: 1280px;
  margin: auto;
}
header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 30px 0;
}
header img {
  width: 60px;
  height: 60px;
  object-fit: contain;
}
h1 {
  font-size: 26px;
  margin: 0 0 8px;
}
p,
small {
  color: var(--el-text-color-secondary);
}
.hm-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}
.hm-summary button {
  text-align: left;
  background: var(--el-bg-color);
  color: var(--el-text-color-primary);
  border: 1px solid var(--el-border-color-light);
  padding: 24px;
  border-radius: 8px;
  cursor: pointer;
}
strong {
  display: block;
  font-size: 34px;
  margin: 16px 0;
  font-weight: 600;
}
.hm-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
