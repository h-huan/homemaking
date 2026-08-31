<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="Area" prop="areaName"><el-input v-model="queryParams.areaName" placeholder="Area name" clearable style="width: 220px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="Status" prop="status"><el-select v-model="queryParams.status" placeholder="Status" clearable style="width: 160px"><el-option label="Enabled" value="0" /><el-option label="Disabled" value="1" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">Search</el-button><el-button icon="Refresh" @click="resetQuery">Reset</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8"><el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['hm:area:add']">Add</el-button></el-col><el-col :span="1.5"><el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['hm:area:edit']">Edit</el-button></el-col><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>
    <el-table v-loading="loading" :data="areaList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="ID" prop="areaId" width="90" />
      <el-table-column label="Area Name" prop="areaName" min-width="160" />
      <el-table-column label="Province" prop="provinceName" min-width="120" />
      <el-table-column label="City" prop="cityName" min-width="120" />
      <el-table-column label="District" prop="districtName" min-width="120" />
      <el-table-column label="Extra Fee" prop="extraFee" width="120" />
      <el-table-column label="Status" width="100"><template #default="scope">{{ scope.row.status === '0' ? 'Enabled' : 'Disabled' }}</template></el-table-column>
      <el-table-column label="Action" width="140" fixed="right"><template #default="scope"><el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hm:area:edit']">Edit</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="open" :title="title" width="720px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="Area Name" prop="areaName"><el-input v-model="form.areaName" /></el-form-item>
        <el-form-item label="Province Name"><el-input v-model="form.provinceName" /></el-form-item>
        <el-form-item label="Province Code"><el-input v-model="form.provinceCode" /></el-form-item>
        <el-form-item label="City Name"><el-input v-model="form.cityName" /></el-form-item>
        <el-form-item label="City Code"><el-input v-model="form.cityCode" /></el-form-item>
        <el-form-item label="District Name"><el-input v-model="form.districtName" /></el-form-item>
        <el-form-item label="District Code"><el-input v-model="form.districtCode" /></el-form-item>
        <el-form-item label="Org ID"><el-input-number v-model="form.orgId" :min="0" /></el-form-item>
        <el-form-item label="Extra Fee"><el-input-number v-model="form.extraFee" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="Status"><el-radio-group v-model="form.status"><el-radio value="0">Enabled</el-radio><el-radio value="1">Disabled</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="submitForm">Confirm</el-button><el-button @click="cancel">Cancel</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup name="HmArea">
import { addArea, getArea, listArea, updateArea } from '@/api/hm/area'
const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const areaList = ref([])
const ids = ref([])
const single = ref(true)
const data = reactive({ queryParams: { pageNum: 1, pageSize: 10, areaName: undefined, status: undefined }, form: {}, rules: { areaName: [{ required: true, message: 'Area name is required', trigger: 'blur' }] } })
const { queryParams, form, rules } = toRefs(data)
function emptyForm() { return { areaId: undefined, areaName: '', provinceName: '', provinceCode: '', cityName: '', cityCode: '', districtName: '', districtCode: '', orgId: 0, extraFee: 0, status: '0' } }
function getList() { loading.value = true; listArea(queryParams.value).then(res => { areaList.value = res.rows; total.value = res.total }).finally(() => loading.value = false) }
function reset() { form.value = emptyForm(); proxy.resetForm('formRef') }
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.areaId); single.value = selection.length !== 1 }
function handleAdd() { reset(); title.value = 'Add Area'; open.value = true }
function handleUpdate(row) { const areaId = row?.areaId || ids.value[0]; if (!areaId) return; reset(); getArea(areaId).then(res => { form.value = { ...emptyForm(), ...res.data }; title.value = 'Edit Area'; open.value = true }) }
function submitForm() { proxy.$refs.formRef.validate(valid => { if (!valid) return; const action = form.value.areaId ? updateArea : addArea; action(form.value).then(() => { proxy.$modal.msgSuccess('Saved successfully'); open.value = false; getList() }) }) }
getList()
</script>