<template>
  <HmPage
    eyebrow="HM · 服务人员工作台"
    title="我的工作台"
    description="安排服务日程，处理到家任务，查看自己的收入与结算记录。"
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="我的任务" name="tasks">
        <el-button class="mb-12px mr-12px" @click="load">刷新任务</el-button>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          :clearable="false"
          @change="load"
        />
        <el-timeline class="tasks"
          ><el-timeline-item
            v-for="task in tasks"
            :key="task.id"
            :timestamp="formatDate(task.starts_at) + ' — ' + formatDate(task.ends_at)"
            placement="top"
          >
            <el-card shadow="never"
              ><div class="task-head"
                ><div
                  ><strong>{{ task.service_name }}</strong
                  ><p>{{ task.address }}</p
                  ><p v-if="task.customer_remark">客户备注：{{ task.customer_remark }}</p></div
                ><el-tag>{{
                  stateNames[task.fulfillment_status] || task.fulfillment_status
                }}</el-tag></div
              >
              <el-space wrap>
                <el-button
                  v-if="task.fulfillment_status === 'ARRIVED'"
                  :disabled="!can('worker:fulfill')"
                  @click="openEvidence(task.id, 'BEFORE')"
                  >上传服务前照片</el-button
                >
                <el-button
                  v-if="task.fulfillment_status === 'STARTED'"
                  :disabled="!can('worker:fulfill')"
                  @click="openEvidence(task.id, 'AFTER')"
                  >上传服务后照片</el-button
                >
                <el-button
                  v-for="action in actions(task.fulfillment_status)"
                  :key="action.value"
                  :type="action.primary ? 'primary' : 'default'"
                  :disabled="!can('worker:fulfill')"
                  @click="act(task.id, action.value)"
                  >{{ action.label }}</el-button
                >
                <el-button @click="viewPhotos(task.id)">查看照片</el-button>
              </el-space>
            </el-card>
          </el-timeline-item></el-timeline
        >
        <el-empty v-if="!tasks.length" description="所选日期暂无已付款任务" />
      </el-tab-pane>
      <el-tab-pane label="排班与请假" name="calendar"
        ><WorkerCalendar v-if="activeTab === 'calendar'" :can-apply="can('worker:leave')"
      /></el-tab-pane>
      <el-tab-pane v-if="can('worker:income')" label="我的收入" name="income"
        ><WorkerIncome v-if="activeTab === 'income'"
      /></el-tab-pane>
    </el-tabs>
    <el-dialog v-model="evidence.visible" title="上传履约照片" width="min(480px, 94vw)">
      <p>仅支持 JPEG / PNG，单张不超过 5 MB。请避免拍摄无关人员及私人信息。</p>
      <input :key="fileInputKey" type="file" accept="image/jpeg,image/png" @change="choosePhoto" />
      <img v-if="localPreview" :src="localPreview" class="photo-preview" alt="待上传照片" />
      <el-input
        v-model="evidence.note"
        type="textarea"
        maxlength="500"
        placeholder="照片说明（选填）"
      />
      <template #footer
        ><el-button @click="evidence.visible = false">取消</el-button
        ><el-button
          type="primary"
          :loading="uploading"
          :disabled="!can('worker:fulfill')"
          @click="saveEvidence"
          >保存照片</el-button
        ></template
      >
    </el-dialog>
    <el-dialog v-model="photosVisible" title="履约照片" width="min(820px,94vw)"
      ><div class="photos"
        ><figure v-for="photo in photos" :key="photo.id"
          ><el-image
            :src="photo.url"
            :preview-src-list="photos.map((p) => p.url)"
            fit="cover"
          /><figcaption
            >{{ photo.phase === 'BEFORE' ? '服务前' : '服务后' }} · {{ photo.note }}</figcaption
          ></figure
        ></div
      ><el-empty v-if="!photos.length" description="尚未上传照片"
    /></el-dialog>
  </HmPage>
</template>
<script setup lang="ts">
import dayjs from 'dayjs'
import { onMounted, onBeforeUnmount, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/homemaking'
import HmPage from './components/HmPage.vue'
import WorkerCalendar from './components/WorkerCalendar.vue'
import WorkerIncome from './components/WorkerIncome.vue'
import { formatDate } from '@/utils/formatTime'
const activeTab = ref('tasks')
import { useHmAccess } from './useAccess'
const { can, loadAccess } = useHmAccess()
defineOptions({ name: 'HomemakingWorker' })
const range = ref([dayjs().format('YYYY-MM-DD'), dayjs().add(7, 'day').format('YYYY-MM-DD')]),
  tasks = ref<any[]>([]),
  uploading = ref(false)
const evidence = reactive({ visible: false, orderId: 0, phase: 'BEFORE', note: '' }),
  fileInputKey = ref(0),
  localPreview = ref(''),
  selectedFile = ref<File>()
const photosVisible = ref(false),
  photos = ref<any[]>([])
const stateNames: Record<string, string> = {
  WAITING: '等待接单',
  ACCEPTED: '已接单',
  ARRIVED: '已到达',
  STARTED: '服务中',
  COMPLETED: '已完成'
}
async function load() {
  const data = await api.workerTasks(range.value[0], range.value[1])
  tasks.value = data.tasks || []
}
function actions(s: string) {
  return s === 'WAITING'
    ? [
        { label: '接单', value: 'ACCEPT', primary: true },
        { label: '拒绝', value: 'REJECT' }
      ]
    : s === 'ACCEPTED'
      ? [
          { label: '到达打卡', value: 'ARRIVE', primary: true },
          { label: '上报异常', value: 'EXCEPTION' }
        ]
      : s === 'ARRIVED'
        ? [
            { label: '开始服务', value: 'START', primary: true },
            { label: '上报异常', value: 'EXCEPTION' }
          ]
        : s === 'STARTED'
          ? [
              { label: '完成服务', value: 'COMPLETE', primary: true },
              { label: '上报异常', value: 'EXCEPTION' }
            ]
          : []
}
function openEvidence(id: number, phase: string) {
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
  localPreview.value = ''
  selectedFile.value = undefined
  fileInputKey.value++
  Object.assign(evidence, { visible: true, orderId: id, phase, note: '' })
}
function choosePhoto(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024)
    return ElMessage.warning('请选择不超过 5 MB 的 JPEG 或 PNG')
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
  selectedFile.value = file
  localPreview.value = URL.createObjectURL(file)
}
async function saveEvidence() {
  if (!selectedFile.value) return ElMessage.warning('请先选择照片')
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', selectedFile.value)
    data.append('phase', evidence.phase)
    data.append('note', evidence.note)
    await api.addWorkerEvidence(evidence.orderId, data)
    evidence.visible = false
    ElMessage.success('履约照片已保存')
  } finally {
    uploading.value = false
  }
}
async function viewPhotos(id: number) {
  photos.value.forEach((p) => URL.revokeObjectURL(p.url))
  photos.value = []
  photosVisible.value = true
  for (const photo of await api.getWorkerEvidence(id)) {
    const blob = await api.evidenceImage(id, photo.id, true)
    photos.value.push({ ...photo, url: URL.createObjectURL(blob) })
  }
}
async function act(id: number, action: string) {
  let note = ''
  if (['REJECT', 'EXCEPTION'].includes(action))
    note = await ElMessageBox.prompt('请填写具体说明', '确认操作', {
      inputValidator: (v) => !!v?.trim() || '请填写说明'
    }).then((v) => v.value)
  await api.workerAction(id, { action, note })
  ElMessage.success(action === 'EXCEPTION' ? '异常已记录，请联系门店跟进' : '任务状态已更新')
  await load()
}
onMounted(async () => {
  await loadAccess()
  await load()
})
onBeforeUnmount(() => {
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
  photos.value.forEach((p) => URL.revokeObjectURL(p.url))
})
</script>
<style scoped>
.tasks {
  max-width: 900px;
  margin-top: 30px;
}
.task-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}
.task-head p {
  color: var(--el-text-color-secondary);
}
.photo-preview {
  display: block;
  max-width: 100%;
  max-height: 260px;
  margin: 20px 0;
}
.photos {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}
.photos figure {
  margin: 0;
}
.photos .el-image {
  width: 100%;
  height: 220px;
}
.photos figcaption {
  margin-top: 10px;
  color: var(--el-text-color-secondary);
}
@media (max-width: 600px) {
  .photos {
    grid-template-columns: 1fr;
  }
}
</style>
