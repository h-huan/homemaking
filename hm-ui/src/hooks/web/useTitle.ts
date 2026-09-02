import { watch, ref } from 'vue'
import { isString } from '@/utils/is'
import { tenantBrand } from '@/hooks/web/useTenantBrand'



export const useTitle = (newTitle?: string) => {
  const { t } = useI18n()
  const title = ref(
    newTitle ? `${tenantBrand.name} - ${t(newTitle as string)}` : tenantBrand.name
  )

  watch(
    title,
    (n, o) => {
      if (isString(n) && n !== o && document) {
        document.title = n
      }
    },
    { immediate: true }
  )

  return title
}
