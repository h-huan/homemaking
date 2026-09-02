<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="Worker"><el-input v-model="queryParams.workerName" placeholder="Worker name" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="Mobile"><el-input v-model="queryParams.mobile" placeholder="Mobile" clearable style="width: 180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">Search</el-button><el-button icon="Refresh" @click="resetQuery">Reset</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['hm:worker:add']">Add</el-button></el-col><el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['hm:worker:edit']">Edit</el-button></el-col><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="workerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" prop="workerProfileId" width="90" />
      <el-table-column label="Name" prop="workerName" min-width="140" />
      <el-table-column label="Mobile" prop="mobile" min-width="140" />
      <el-table-column label="Type" prop="workerType" min-width="120" />
      <el-table-column label="Star" prop="serviceStar" width="100" />
      <el-table-column label="Services" prop="serviceCount" width="100" />
      <el-table-column label="Status" width="100"><template #default="scope">{{ scope.row.status === '0' ? 'Enabled' : 'Disabled' }}</template></el-table-column>
      <el-table-column label="Action" width="140" fixed="right"><template #default="scope"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hm:worker:edit']">Edit</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="open" :title="title" width="860px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="Worker Name" prop="workerName"><el-input v-model="form.workerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Mobile" prop="mobile"><el-input v-model="form.mobile" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Gender"><el-select v-model="form.gender"><el-option label="Unknown" value="0" /><el-option label="Male" value="1" /><el-option label="Female" value="2" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Worker Type"><el-input v-model="form.workerType" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Avatar"><el-input v-model="form.avatar" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="ID Card"><el-input v-model="form.idCardNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Org ID"><el-input-number v-model="form.orgId" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Service Star"><el-input-number v-model="form.serviceStar" :min="0" :max="5" :precision="1" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Service Count"><el-input-number v-model="form.serviceCount" :min="0" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Employment"><el-select v-model="form.employmentStatus"><el-option label="Onboard" value="0" /><el-option label="Left" value="1" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Work Status"><el-select v-model="form.workStatus"><el-option label="Idle" value="0" /><el-option label="Busy" value="1" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="Status"><el-radio-group v-model="form.status"><el-radio value="0">Enabled</el-radio><el-radio value="1">Disabled</el-radio></el-radio-group></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="Service Items"><el-select v-model="form.serviceItemIdList" multiple style="width: 100%"><el-option v-for="item in serviceOptions" :key="item.serviceItemId" :label="item.serviceName" :value="item.serviceItemId" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="Area Coverage"><el-select v-model="form.areaIdList" multiple style="width: 100%"><el-option v-for="item in areaOptions" :key="item.areaId" :label="item.areaName" :value="item.areaId" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="Intro"><el-input v-model="form.intro" type="textarea" :rows="4" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">Confirm</el-button><el-button @click="cancel">Cancel</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup name="HmWorker">
import { listArea } from '@/api/hm/area'
import { listService } from '@/api/hm/service'
import { addWorker, getWorker, listWorker, updateWorker } from '@/api/hm/worker'
const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const workerList = ref([])
const serviceOptions = ref([])
const areaOptions = ref([])
const ids = ref([])
const single = ref(true)
const data = reactive({ queryParams: { pageNum: 1, pageSize: 10, workerName: undefined, mobile: undefined }, form: {}, rules: { workerName: [{ required: true, message: 'Worker name is required', trigger: 'blur' }], mobile: [{ required: true, message: 'Mobile is required', trigger: 'blur' }] } })
const { queryParams, form, rules } = toRefs(data)
function emptyForm() { return { workerProfileId: undefined, workerName: '', mobile: '', gender: '0', avatar: '', idCardNo: '', workerType: '', intro: '', serviceStar: 5, serviceCount: 0, employmentStatus: '0', workStatus: '0', orgId: 0, status: '0', serviceItemIdList: [], areaIdList: [] } }
function loadOptions() { listService({ pageNum: 1, pageSize: 1000 }).then(res => serviceOptions.value = res.rows || []); listArea({ pageNum: 1, pageSize: 1000 }).then(res => areaOptions.value = res.rows || []) }
function getList() { loading.value = true; listWorker(queryParams.value).then(res => { workerList.value = res.rows; total.value = res.total }).finally(() => loading.value = false) }
function reset() { form.value = emptyForm(); proxy.resetForm('formRef') }
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.workerProfileId); single.value = selection.length !== 1 }
function handleAdd() { reset(); title.value = 'Add Worker'; open.value = true }
function handleUpdate(row) { const workerId = row?.workerProfileId || ids.value[0]; if (!workerId) return; reset(); getWorker(workerId).then(res => { form.value = { ...emptyForm(), ...res.data }; form.value.serviceItemIdList = form.value.serviceItemIdList || []; form.value.areaIdList = form.value.areaIdList || []; title.value = 'Edit Worker'; open.value = true }) }
function submitForm() { proxy.$refs.formRef.validate(valid => { if (!valid) return; const action = form.value.workerProfileId ? updateWorker : addWorker; action(form.value).then(() => { proxy.$modal.msgSuccess('Saved successfully'); open.value = false; getList() }) }) }
loadOptions(); getList()
</script>