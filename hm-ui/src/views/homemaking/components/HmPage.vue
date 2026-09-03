<template>
  <section class="hm-page"
    ><header
      ><div
        ><p>{{ eyebrow }}</p
        ><h1>{{ title }}</h1
        ><span v-if="description">{{ description }}</span></div
      ><slot name="actions" /></header
    ><slot
  /></section>
</template>
<script setup lang="ts">
defineProps<{ title: string; eyebrow: string; description?: string }>()
</script>
<style scoped>
.hm-page {
  padding: 28px;
  background: var(--el-bg-color);
  border-radius: 8px;
}
.hm-page > header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  margin-bottom: 30px;
}
.hm-page > header p {
  margin: 0 0 8px;
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 2px;
}
.hm-page > header h1 {
  margin: 0 0 8px;
  font-size: 25px;
  font-weight: 600;
}
.hm-page :deep(.hm-hint) {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
.hm-page > header span {
  color: var(--el-text-color-secondary);
}
@media (max-width: 700px) {
  .hm-page {
    padding: 16px;
  }
  .hm-page > header {
    display: block;
  }
}
</style>
