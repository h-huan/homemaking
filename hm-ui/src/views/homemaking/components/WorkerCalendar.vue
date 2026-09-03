<template>
  <el-space wrap class="mb-20px">
    <el-date-picker
      v-model="range"
      type="daterange"
      value-format="YYYY-MM-DD"
      :clearable="false"
      @change="load"
    />
    <el-button @click="load">刷新日历</el-button>
    <el-button v-if="canApply" type="primary" @click="openRequest">申请请假 / 休息</el-button>
  </el-space>
  <el-alert
    class="mb-20px"
    title="待审核申请不占用可预约时间；批准后该时段停止接单。已有预约须先由门店调整。"
    type="info"
    :closable="false"
  />
  <el-tabs v-model="view">
    <el-tab-pane label="我的排班" name="schedule">
      <el-table :data="calendar.schedules" empty-text="所选日期暂无排班，请联系门店安排">
        <el-table-column label="开始"
          ><template #default="{ row }">{{ formatDate(row.starts_at) }}</template></el-table-column
        >
        <el-table-column label="结束"
          ><template #default="{ row }">{{ formatDate(row.ends_at) }}</template></el-table-column
        >
      </el-table>
    </el-tab-pane>
    <el-tab-pane label="可服务时间" name="available">
      <p class="hm-hint"
        >以下时段已扣除已批准的请假、休息和预约占用，具体接单还需匹配项目技能及服务区域。</p
      >
      <el-table :data="calendar.available" empty-text="所选日期暂无可服务时间">
        <el-table-column label="开始"
          ><template #default="{ row }">{{ formatDate(row.starts_at) }}</template></el-table-column
        >
        <el-table-column label="结束"
          ><template #default="{ row }">{{ formatDate(row.ends_at) }}</template></el-table-column
        >
      </el-table>
    </el-tab-pane>
    <el-tab-pane label="请假与休息" name="leave"
      ><TimeOffTable :rows="calendar.leaves || []" :can-cancel="canApply" @cancel="cancel"
    /></el-tab-pane>
  </el-tabs>
  <el-dialog
    v-model="visible"
    title="申请请假 / 休息"
    width="min(520px,94vw)"
    :close-on-click-modal="false"
  >
    <el-form label-position="top">
      <el-form-item label="申请类型"
        ><el-radio-group v-model="form.kind"
          ><el-radio value="LEAVE">请假</el-radio
          ><el-radio value="REST">休息</el-radio></el-radio-group
        ></el-form-item
      >
      <el-form-item label="开始和结束时间（半小时刻度，单次最多 31 天）"
        ><el-date-picker
          v-model="interval"
          type="datetimerange"
          value-format="YYYY-MM-DDTHH:mm:ss"
          class="!w-full"
      /></el-form-item>
      <el-form-item label="申请原因"
        ><el-input v-model="form.reason" type="textarea" maxlength="500" show-word-limit :rows="3"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="submit">提交申请</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { onMounted, ref } from 'vue'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/homemaking'
import { formatDate } from '@/utils/formatTime'
import TimeOffTable from './TimeOffTable.vue'
defineProps<{ canApply: boolean }>()
const range = ref([dayjs().format('YYYY-MM-DD'), dayjs().add(30, 'day').format('YYYY-MM-DD')])
const calendar = ref<any>({}),
  view = ref('schedule'),
  visible = ref(false),
  saving = ref(false)
const interval = ref<string[]>([]),
  form = ref({ kind: 'LEAVE', reason: '', requestKey: '' })
let loadSequence = 0
async function load() {
  const sequence = ++loadSequence
  const data = await api.workerCalendar(range.value[0], range.value[1])
  if (sequence === loadSequence) calendar.value = data
}
function openRequest() {
  form.value = { kind: 'LEAVE', reason: '', requestKey: crypto.randomUUID() }
  interval.value = []
  visible.value = true
}
async function submit() {
  if (interval.value?.length !== 2 || !form.value.reason.trim())
    return ElMessage.warning('请选择时间并填写原因')
  if (interval.value.some((t) => dayjs(t).minute() % 30 || dayjs(t).second()))
    return ElMessage.warning('时间须以半小时为单位')
  saving.value = true
  try {
    await api.workerRequestLeave({
      ...form.value,
      startsAt: interval.value[0],
      endsAt: interval.value[1]
    })
    visible.value = false
    view.value = 'leave'
    range.value = [interval.value[0].slice(0, 10), interval.value[1].slice(0, 10)]
    ElMessage.success('申请已提交，等待门店审核')
    await load()
  } finally {
    saving.value = false
  }
}
async function cancel(row: any) {
  const { value } = await ElMessageBox.prompt('撤销后保留处理记录。请填写原因。', '撤销申请', {
    inputValidator: (v) => (!!v?.trim() && v.length <= 500) || '请填写 1–500 字原因'
  })
  try {
    await api.workerCancelLeave(row.id, { version: row.version, reason: value })
    ElMessage.success('申请已撤销')
  } finally {
    await load()
  }
}
onMounted(load)
</script>
