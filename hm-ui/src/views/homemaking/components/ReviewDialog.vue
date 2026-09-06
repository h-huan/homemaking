<template>
  <el-dialog v-model="visible" title="评价详情与处理" width="min(760px, 94vw)" destroy-on-close>
    <el-skeleton v-if="loading" :rows="7" animated />
    <template v-else-if="detail">
      <el-descriptions :column="descriptionColumns" border>
        <el-descriptions-item label="订单">#{{ detail.order_id }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.nickname || `#${detail.customer_id}` }}</el-descriptions-item>
        <el-descriptions-item label="服务质量">
          <el-rate :model-value="detail.service_rating" disabled show-score score-template="{value}" />
        </el-descriptions-item>
        <el-descriptions-item label="服务人员">
          <el-rate :model-value="detail.worker_rating" disabled show-score score-template="{value}" />
        </el-descriptions-item>
        <el-descriptions-item label="评价内容" :span="descriptionColumns">
          <div class="review-copy">{{ detail.content }}</div>
          <el-space v-if="detail.tags?.length" wrap class="review-tags">
            <el-tag v-for="tag in detail.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
          </el-space>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="images.length" class="image-grid">
        <el-image
          v-for="image in images"
          :key="image.id"
          :src="image.url"
          :preview-src-list="images.map((item) => item.url)"
          fit="cover"
          hide-on-click-modal
        />
      </div>

      <el-form v-if="canReply" class="review-form" label-position="top">
        <el-form-item label="商家回复">
          <el-input v-model="reply" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-button type="primary" :loading="savingReply" @click="saveReply">保存回复</el-button>
      </el-form>
      <div v-else-if="detail.reply_content" class="merchant-reply">
        <strong>商家回复</strong><p>{{ detail.reply_content }}</p>
      </div>

      <el-form v-if="canModerate" class="review-form moderation" label-position="top">
        <el-form-item label="展示设置">
          <el-switch v-model="moderation.visible" active-text="允许公开展示" />
          <el-switch
            v-model="moderation.recommended"
            active-text="设为推荐评价"
            :disabled="!moderation.visible"
          />
        </el-form-item>
        <el-alert
          title="隐藏评价时会同时取消推荐；推荐评价会优先出现在小程序和官网。"
          type="info"
          :closable="false"
        />
        <el-button class="moderation-save" :loading="savingModeration" @click="saveModeration"
          >保存展示设置</el-button
        >
      </el-form>

      <el-timeline v-if="detail.logs?.length" class="timeline">
        <el-timeline-item
          v-for="(item, index) in detail.logs"
          :key="index"
          :timestamp="formatDate(item.created_at)"
          placement="top"
        >
          <strong>{{ actionLabel(item.action) }}</strong>
          <p>{{ item.detail }}</p>
        </el-timeline-item>
      </el-timeline>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import { formatDate } from '@/utils/formatTime'
import * as api from '@/api/homemaking'

const props = defineProps<{
  modelValue: boolean
  target?: api.BusinessRow
  canReply: boolean
  canModerate: boolean
}>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void; (e: 'saved'): void }>()
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
const { width } = useWindowSize()
const descriptionColumns = computed(() => (width.value < 640 ? 1 : 2))
const detail = ref<api.BusinessRow>()
const images = ref<Array<api.BusinessRow & { url: string }>>([])
const reply = ref('')
const moderation = ref({ visible: false, recommended: false })
const loading = ref(false)
const savingReply = ref(false)
const savingModeration = ref(false)

function clearImages() {
  images.value.forEach((image) => URL.revokeObjectURL(image.url))
  images.value = []
}
async function load() {
  if (!props.target) return
  loading.value = true
  clearImages()
  try {
    detail.value = await api.getReview(props.target.id)
    reply.value = detail.value?.reply_content || ''
    moderation.value = {
      visible: !!detail.value?.visible,
      recommended: !!detail.value?.recommended
    }
    for (const image of detail.value?.images || []) {
      const blob = await api.reviewImage(detail.value!.id, image.id)
      images.value.push({ ...image, url: URL.createObjectURL(blob) })
    }
  } finally {
    loading.value = false
  }
}
watch(
  () => props.modelValue,
  async (open) => {
    if (open) await load()
    else clearImages()
  }
)
watch(
  () => moderation.value.visible,
  (shown) => {
    if (!shown) moderation.value.recommended = false
  }
)
async function saveReply() {
  const content = reply.value.trim()
  if (!content) return ElMessage.warning('请填写商家回复')
  savingReply.value = true
  try {
    await api.replyReview(detail.value!.id, content)
    ElMessage.success('商家回复已保存')
    await load()
    emit('saved')
  } finally {
    savingReply.value = false
  }
}
async function saveModeration() {
  savingModeration.value = true
  try {
    await api.moderateReview(detail.value!.id, moderation.value)
    ElMessage.success('展示设置已保存')
    await load()
    emit('saved')
  } finally {
    savingModeration.value = false
  }
}
const actionLabel = (action: string) =>
  ({
    DRAFT_SAVED: '客户保存草稿',
    IMAGE_ADDED: '客户上传图片',
    PUBLISHED: '客户发布评价',
    REPLIED: '商家回复',
    MODERATED: '展示设置变更',
    MIGRATED: '历史评价迁入'
  })[action] || action
onBeforeUnmount(clearImages)
</script>

<style scoped>
.review-copy { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.7; }
.review-tags { margin-top: 12px; }
.image-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(150px, 100%), 1fr)); gap: 14px; margin-top: 22px; }
.image-grid .el-image { width: 100%; aspect-ratio: 1; border-radius: 14px; background: var(--el-fill-color-light); }
.review-form, .merchant-reply { margin-top: 24px; padding: clamp(16px, 3vw, 22px); border-radius: 16px; background: var(--el-fill-color-light); }
.merchant-reply p, .timeline p { margin: 6px 0 0; color: var(--el-text-color-secondary); white-space: pre-wrap; overflow-wrap: anywhere; }
.moderation :deep(.el-form-item__content) { display: flex; flex-wrap: wrap; gap: 18px; }
.moderation-save { margin-top: 16px; }
.timeline { margin-top: 28px; }
@media (max-width: 639px) {
  .image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .review-form :deep(.el-button) { width: 100%; }
}
</style>
