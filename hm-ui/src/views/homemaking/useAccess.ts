import { ref } from 'vue'
import request from '@/config/axios'

// Server returns the intersection of role template and system permissions, not just menu visibility.
export function useHmAccess() {
  const permissions = ref<string[]>([])
  const range = ref('')
  const can = (permission: string) => permissions.value.includes(`homemaking:${permission}`)
  async function loadAccess() {
    const data = await request.get({ url: '/homemaking/access/me' })
    permissions.value = data.permissions
    range.value = data.range
  }
  return { can, range, loadAccess }
}
