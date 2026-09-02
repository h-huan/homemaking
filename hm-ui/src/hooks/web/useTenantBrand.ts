import { reactive } from 'vue'
import { useAppStoreWithOut } from '@/store/modules/app'
export const tenantBrand = reactive({
  name: 'HM 家政管理平台',
  logo: '/hm-logo.svg',
  loginTitle: '欢迎使用 HM 家政',
  loginBackground: '',
  homeModules: ['services'] as string[]
})
export function applyTenantBrand(data: Record<string, any>) {
  if (!data?.brand_name) return
  tenantBrand.name = data.brand_name
  tenantBrand.logo = data.logo || '/hm-logo.svg'
  tenantBrand.loginTitle = data.login_title || data.brand_name
  tenantBrand.loginBackground = data.login_background || ''
  try {
    tenantBrand.homeModules =
      typeof data.home_modules === 'string'
        ? JSON.parse(data.home_modules)
        : data.home_modules || ['services']
  } catch {
    tenantBrand.homeModules = ['services']
  }
  document.title = tenantBrand.name
  if (/^#[0-9a-fA-F]{6}$/.test(data.primary_color)) {
    const theme = useAppStoreWithOut()
    theme.setTheme({ elColorPrimary: data.primary_color, leftMenuBgActiveColor: data.primary_color })
    theme.setCssVarTheme()
  }
  const icon = document.querySelector<HTMLLinkElement>('link[rel="icon"]')
  if (icon) icon.href = data.favicon || '/hm-logo.svg'
}
export async function loadPublicBrand() {
  try {
    const response = await fetch(`${import.meta.env.VITE_BASE_URL}/app-api/homemaking/public/brand`)
    const result = await response.json()
    if (result.code === 0) applyTenantBrand(result.data)
  } catch {
    /* The default HM identity remains available while the server is unavailable. */
  }
}
