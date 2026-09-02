<template>
  <main class="wechat-result"
    ><img :src="tenantBrand.logo" alt="" /><h1>{{ tenantBrand.name }}</h1
    ><p role="status">{{ message }}</p
    ><p v-if="done">可返回公众号继续使用服务。</p></main
  >
</template>
<script setup lang="ts">
import { tenantBrand, applyTenantBrand } from '@/hooks/web/useTenantBrand'
const message = ref('正在验证微信身份…'),
  done = ref(false)
onMounted(async () => {
  try {
    const base = import.meta.env.VITE_BASE_URL
    const result = await (
      await fetch(`${base}/app-api/homemaking/public/brand`, { credentials: 'include' })
    ).json()
    if (result.code !== 0 || !result.data.mp_app_id) throw new Error('当前公众号尚未配置')
    applyTenantBrand(result.data)
    const query = new URLSearchParams(location.search)
    const response = await (
      await fetch(`${base}/app-api/homemaking/public/wechat/mp-login`, {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json', 'tenant-id': String(result.data.tenant_id) },
        body: JSON.stringify({
          appId: result.data.mp_app_id,
          code: query.get('code'),
          state: query.get('state')
        })
      })
    ).json()
    if (response.code !== 0) throw new Error(response.msg || '微信验证失败，请从公众号重新进入')
    sessionStorage.setItem('hm-customer-session', JSON.stringify(response.data))
    history.replaceState(null, '', location.pathname)
    done.value = true
    message.value = '微信身份已关联'
  } catch (error) {
    message.value = error instanceof Error ? error.message : '微信验证失败，请重新进入'
  }
})
</script>
<style scoped>
.wechat-result {
  max-width: 500px;
  margin: 15vh auto;
  padding: 30px;
  text-align: center;
}
img {
  width: 70px;
  height: 70px;
  object-fit: contain;
}
h1 {
  font-size: 24px;
}
p {
  line-height: 1.8;
}
</style>
