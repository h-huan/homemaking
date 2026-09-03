<template>
  <HmPage
    eyebrow="HM · 排班中心"
    title="服务人员能力与日历"
    description="人员必须同时满足服务项目、行政区、班次及空闲时段，才会出现在客户的可预约产能中。"
  >
    <template #actions><el-button @click="loadWorkers">刷新人员</el-button></template>
    <div class="toolbar"
      ><el-select v-model="workerId" filterable placeholder="选择服务人员" @change="load"
        ><el-option v-for="w in workers" :key="w.id" :value="w.id" :label="w.name" /></el-select
      ><el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        :clearable="false"
        @change="load"
    /></div>
    <el-empty v-if="!workers.length" description="请先在运营台新增服务人员" />
    <el-row v-else :gutter="20">
      <el-col :md="9" :xs="24"
        ><el-card shadow="never"
          ><template #header>能力与账号</template>
          <el-form label-position="top">
            <el-form-item label="可服务项目"
              ><el-select v-model="skills.serviceIds" multiple filterable
                ><el-option
                  v-for="service in storeServices"
                  :key="service.id"
                  :label="service.name"
                  :value="service.id" /></el-select
            ></el-form-item>
            <el-form-item label="行政区编号"
              ><el-select
                v-model="skills.districts"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="填写行政区编号，全部区域选择 *"
                ><el-option label="全部区域（明确允许）" value="*" /></el-select
            ></el-form-item>
            <el-button type="primary" :disabled="!can('schedule:write')" @click="saveSkills"
              >保存能力</el-button
            >
            <el-divider />
            <el-form-item label="绑定后台用户编号"
              ><el-input-number v-model="userId" :min="1"
            /></el-form-item>
            <p class="muted">请先创建同租户后台用户，并为其角色授予“服务人员工作台”权限。</p>
            <el-button :disabled="!can('workers:bind')" @click="bind">绑定工作台账号</el-button>
          </el-form>
        </el-card></el-col
      >
      <el-col :md="15" :xs="24"
        ><el-card shadow="never"
          ><template #header>班次与请假</template>
          <el-form label-position="top"
            ><el-form-item label="开始和结束时间（同一天，半小时刻度）"
              ><el-date-picker
                v-model="interval"
                type="datetimerange"
                value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item
            ><el-form-item label="请假原因"
              ><el-input v-model="reason" maxlength="500" /></el-form-item
            ><el-space
              ><el-button
                type="primary"
                :disabled="!can('schedule:write')"
                @click="add('schedules')"
                >新增班次</el-button
              ><el-button :disabled="!can('schedule:review')" @click="add('leaves')"
                >登记请假</el-button
              ></el-space
            ></el-form
          >
          <el-divider />
          <el-form label-position="top"
            ><el-form-item label="使用班次模板"
              ><el-select v-model="templateId" placeholder="选择模板"
                ><el-option
                  v-for="item in templates"
                  :key="item.id"
                  :value="item.id"
                  :label="
                    item.name + ' · ' + item.starts_at + '—' + item.ends_at
                  " /></el-select></el-form-item
            ><el-form-item label="应用日期（最多 31 天）"
              ><el-date-picker
                v-model="shiftDates"
                type="dates"
                value-format="YYYY-MM-DD" /></el-form-item
            ><el-space
              ><el-button :disabled="!can('schedule:write')" @click="applyTemplate"
                >批量安排</el-button
              ><el-button
                text
                type="primary"
                :disabled="!can('schedule:templates')"
                @click="templateVisible = true"
                >新建模板</el-button
              ></el-space
            ></el-form
          >
        </el-card></el-col
      >
    </el-row>
    <el-card v-if="workerId" shadow="never"
      ><template #header>日历记录</template
      ><el-table :data="events" empty-text="所选日期暂无排班"
        ><el-table-column prop="kind" label="类型" width="90" /><el-table-column
          prop="starts_at"
          label="开始"
          min-width="180"
          ><template #default="{ row }">{{ formatDate(row.starts_at) }}</template></el-table-column
        ><el-table-column prop="ends_at" label="结束" min-width="180"
          ><template #default="{ row }">{{ formatDate(row.ends_at) }}</template></el-table-column
        ><el-table-column prop="service_name" label="预约服务" /><el-table-column
          prop="reason"
          label="说明"
        /><el-table-column label="操作" width="90"
          ><template #default="{ row }"
            ><el-button
              v-if="row.source"
              text
              type="danger"
              :disabled="!can('schedule:write')"
              @click="remove(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      ></el-card
    >
    <el-card v-if="workerId" shadow="never"
      ><template #header>请假与休息审批</template>
      <p class="hm-hint"
        >待审核申请不影响预约。批准前请处理冲突订单；展开记录可查看完整处理经过。</p
      >
      <TimeOffTable
        :rows="calendar.leaves || []"
        :can-review="can('schedule:review')"
        :can-cancel="can('schedule:review')"
        @review="reviewLeave"
        @cancel="cancelLeave"
      />
    </el-card>
    <el-dialog v-model="templateVisible" title="新建班次模板" width="min(460px,94vw)"
      ><el-form label-position="top"
        ><el-form-item label="模板名称"
          ><el-input v-model="templateForm.name" maxlength="100" /></el-form-item
        ><el-form-item label="每日时间"
          ><el-time-picker
            v-model="templateTimes"
            is-range
            value-format="HH:mm:ss" /></el-form-item></el-form
      ><template #footer
        ><el-button type="primary" :disabled="!can('schedule:templates')" @click="createTemplate"
          >保存模板</el-button
        ></template
      ></el-dialog
    >
  </HmPage>
</template>
<script setup lang="ts">
import dayjs from 'dayjs'
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as api from '@/api/homemaking'
import HmPage from './components/HmPage.vue'
import TimeOffTable from './components/TimeOffTable.vue'
import { formatDate } from '@/utils/formatTime'
import { useHmAccess } from './useAccess'
const { can, loadAccess } = useHmAccess()
defineOptions({ name: 'HomemakingScheduling' })
const workers = ref<any[]>([]),
  services = ref<any[]>([]),
  workerId = ref<number>(),
  userId = ref(1),
  calendar = ref<any>({ schedules: [], leaves: [], bookings: [], serviceIds: [], districts: [] })
const skills = ref<any>({ serviceIds: [], districts: [] }),
  reason = ref(''),
  interval = ref<string[]>([]),
  templates = ref<any[]>([]),
  templateId = ref<number>(),
  shiftDates = ref<string[]>([])
const templateVisible = ref(false),
  templateForm = ref({ name: '' }),
  templateTimes = ref(['09:00:00', '18:00:00'])
const range = ref([dayjs().format('YYYY-MM-DD'), dayjs().add(30, 'day').format('YYYY-MM-DD')])
const storeServices = computed(() =>
  services.value.filter(
    (s) => s.store_id === workers.value.find((w) => w.id === workerId.value)?.store_id
  )
)
const events = computed(() =>
  [
    ...(calendar.value.schedules || []).map((v: any) => ({
      ...v,
      kind: '班次',
      source: 'schedules'
    })),
    ...(calendar.value.bookings || []).map((v: any) => ({ ...v, kind: '预约' }))
  ].sort((a, b) => String(a.starts_at).localeCompare(String(b.starts_at)))
)
async function loadWorkers() {
  const data = await api.listCatalog('workers', { page: 1, size: 100 })
  workers.value = data.list || []
  services.value = (await api.listCatalog('services', { size: 100 })).list
  templates.value = await api.listShiftTemplates()
  if (!workerId.value && workers.value.length) workerId.value = workers.value[0].id
  await load()
}
let loadSequence = 0
async function load() {
  if (!workerId.value) return
  const sequence = ++loadSequence
  const data = await api.getWorkerCalendar(workerId.value, range.value[0], range.value[1])
  if (sequence !== loadSequence) return
  calendar.value = data
  skills.value = {
    serviceIds: calendar.value.serviceIds || [],
    districts: calendar.value.districts || []
  }
}
async function reviewLeave(row: any, approved: boolean) {
  const selectedWorker = workerId.value!
  const { value } = await ElMessageBox.prompt(
    approved
      ? '批准后该时段停止接单。请填写审批说明。'
      : '请填写驳回原因，服务人员可在工作台查看。',
    approved ? '批准申请' : '驳回申请',
    { inputValidator: (v) => (!!v?.trim() && v.length <= 500) || '请填写 1–500 字说明' }
  )
  try {
    await api.reviewWorkerLeave(selectedWorker, row.id, {
      version: row.version,
      approved,
      reason: value
    })
    ElMessage.success(approved ? '申请已批准' : '申请已驳回')
  } finally {
    await load()
  }
}
async function cancelLeave(row: any) {
  const selectedWorker = workerId.value!
  const { value } = await ElMessageBox.prompt(
    '撤销后将恢复该时段的排班可用性，处理记录继续保留。',
    '撤销申请',
    { inputValidator: (v) => (!!v?.trim() && v.length <= 500) || '请填写 1–500 字原因' }
  )
  try {
    await api.cancelWorkerLeave(selectedWorker, row.id, { version: row.version, reason: value })
    ElMessage.success('申请已撤销')
  } finally {
    await load()
  }
}
async function saveSkills() {
  await api.saveWorkerSkills(workerId.value!, skills.value)
  ElMessage.success('人员能力已保存')
  await load()
}
async function bind() {
  await api.bindWorker(workerId.value!, userId.value)
  ElMessage.success('工作台账号已绑定')
}
async function add(kind: 'schedules' | 'leaves') {
  if (!interval.value || interval.value.length !== 2)
    return ElMessage.warning('请选择开始和结束时间')
  await api.addWorkerInterval(workerId.value!, kind, {
    startsAt: interval.value[0],
    endsAt: interval.value[1],
    reason: reason.value
  })
  ElMessage.success(kind === 'leaves' ? '请假已登记' : '班次已添加')
  await load()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除此' + row.kind + '记录？有预约的班次会被保护。', '删除记录')
  await api.removeWorkerInterval(workerId.value!, row.source, row.id)
  await load()
}
async function createTemplate() {
  if (!templateForm.value.name.trim() || !templateTimes.value)
    return ElMessage.warning('请填写模板名称和时间')
  await api.saveShiftTemplate({
    name: templateForm.value.name,
    startsAt: templateTimes.value[0],
    endsAt: templateTimes.value[1]
  })
  templates.value = await api.listShiftTemplates()
  templateVisible.value = false
  ElMessage.success('模板已保存')
}
async function applyTemplate() {
  if (!templateId.value || !shiftDates.value?.length) return ElMessage.warning('请选择模板与日期')
  await api.applyShiftTemplate(workerId.value!, {
    templateId: templateId.value,
    dates: shiftDates.value
  })
  ElMessage.success('班次已批量安排')
  await load()
}
onMounted(async () => {
  await loadAccess()
  await loadWorkers()
})
</script>
<style scoped>
.toolbar {
  display: flex;
  gap: 14px;
  margin-bottom: 22px;
}
.toolbar > .el-select {
  width: 240px;
}
.el-card {
  margin-bottom: 20px;
}
.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.7;
}
@media (max-width: 700px) {
  .toolbar {
    display: grid;
  }
  .toolbar > .el-select {
    width: 100%;
  }
}
</style>
