<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="服务名称">
        <el-input v-model="queryParams.serviceName" placeholder="请输入服务名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="所属分类">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width: 180px">
          <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['hm:service:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['hm:service:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="single" @click="handleDelete()" v-hasPermi="['hm:service:edit']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="serviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" />
      <el-table-column label="服务ID" prop="serviceItemId" width="90" />
      <el-table-column label="服务名称" prop="serviceName" min-width="180" />
      <el-table-column label="所属分类" prop="categoryName" min-width="140" />
      <el-table-column label="销售类型" prop="saleType" width="120" />
      <el-table-column label="起步价" prop="basePrice" width="120" />
      <el-table-column label="时长(分钟)" prop="serviceDuration" width="110" />
      <el-table-column label="小程序展示" width="100">
        <template #default="scope">{{ scope.row.showInMiniapp === '1' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">{{ scope.row.status === '0' ? '启用' : '停用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hm:service:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['hm:service:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-dialog v-model="open" :title="title" width="1100px" append-to-body destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="服务名称" prop="serviceName">
              <el-input v-model="form.serviceName" placeholder="请输入服务名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类">
                <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="form.serviceSubTitle" placeholder="请输入副标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机构ID">
              <el-input-number v-model="form.orgId" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务封面">
              <image-upload v-model="form.serviceCover" :limit="1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面存储源">
              <el-select v-model="form.serviceCoverStorage" style="width: 100%">
                <el-option label="本地" value="local" />
                <el-option label="阿里OSS" value="aliyun_oss" />
                <el-option label="腾讯COS" value="tencent_cos" />
                <el-option label="七牛云" value="qiniu" />
                <el-option label="其它OSS" value="other_oss" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="封面对象键">
              <el-input v-model="form.serviceCoverObjectKey" placeholder="如：service/cover/xxx.png" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="基础价格">
              <el-input-number v-model="form.basePrice" :min="0" :precision="2" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务时长(分钟)">
              <el-input-number v-model="form.serviceDuration" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计量单位">
              <el-input v-model="form.unitName" placeholder="如：次、小时" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售类型">
              <el-select v-model="form.saleType">
                <el-option label="固定服务" value="fixed" />
                <el-option label="SKU套餐" value="sku" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="需人工确认">
              <el-radio-group v-model="form.needManualConfirm">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许指派人员">
              <el-radio-group v-model="form.allowAssignWorker">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="小程序展示">
              <el-radio-group v-model="form.showInMiniapp">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="官网展示">
              <el-radio-group v-model="form.showInPortal">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio value="0">启用</el-radio>
                <el-radio value="1">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务简介">
              <el-input v-model="form.serviceDesc" type="textarea" :rows="2" placeholder="请输入服务简介" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务内容">
              <el-input v-model="form.serviceContent" type="textarea" :rows="4" placeholder="请输入服务内容" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="服务区域">
              <el-select v-model="form.areaIdList" multiple placeholder="请选择服务区域" style="width: 100%">
                <el-option v-for="item in areaOptions" :key="item.areaId" :label="item.areaName" :value="item.areaId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider>SKU 套餐</el-divider>
        <el-button type="primary" link icon="Plus" @click="addSku">新增套餐</el-button>
        <el-table :data="form.skuList" border class="mt8">
          <el-table-column label="套餐名称" min-width="180">
            <template #default="scope"><el-input v-model="scope.row.skuName" /></template>
          </el-table-column>
          <el-table-column label="售价" width="140">
            <template #default="scope"><el-input-number v-model="scope.row.price" :min="0" :precision="2" /></template>
          </el-table-column>
          <el-table-column label="原价" width="140">
            <template #default="scope"><el-input-number v-model="scope.row.originalPrice" :min="0" :precision="2" /></template>
          </el-table-column>
          <el-table-column label="时长" width="140">
            <template #default="scope"><el-input-number v-model="scope.row.durationMinute" :min="0" /></template>
          </el-table-column>
          <el-table-column label="排序" width="120">
            <template #default="scope"><el-input-number v-model="scope.row.sortNo" :min="0" /></template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="scope"><el-button link type="danger" @click="removeSku(scope.$index)">删除</el-button></template>
          </el-table-column>
        </el-table>

        <el-divider>附加项</el-divider>
        <el-button type="primary" link icon="Plus" @click="addExtra">新增附加项</el-button>
        <el-table :data="form.extraItemList" border class="mt8">
          <el-table-column label="名称" min-width="180">
            <template #default="scope"><el-input v-model="scope.row.extraName" /></template>
          </el-table-column>
          <el-table-column label="加价" width="140">
            <template #default="scope"><el-input-number v-model="scope.row.extraPrice" :min="0" :precision="2" /></template>
          </el-table-column>
          <el-table-column label="计费方式" width="140">
            <template #default="scope">
              <el-select v-model="scope.row.chargeType">
                <el-option label="一次性" value="once" />
                <el-option label="按单位" value="per_unit" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="120">
            <template #default="scope"><el-input-number v-model="scope.row.sortNo" :min="0" /></template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="scope"><el-button link type="danger" @click="removeExtra(scope.$index)">删除</el-button></template>
          </el-table-column>
        </el-table>

        <el-divider>预约规则</el-divider>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="提前天数">
              <el-input-number v-model="form.bookingRule.advanceDays" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="最少提前分钟">
              <el-input-number v-model="form.bookingRule.minAdvanceMinutes" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="最远预约天数">
              <el-input-number v-model="form.bookingRule.maxAdvanceDays" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="允许当天预约">
              <el-radio-group v-model="form.bookingRule.allowSameDay">
                <el-radio value="1">是</el-radio>
                <el-radio value="0">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="可约时段JSON">
              <el-input v-model="form.bookingRule.timeSlotsJson" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="取消规则JSON">
              <el-input v-model="form.bookingRule.cancelRuleJson" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="HmService">
import { listArea } from '@/api/hm/area'
import { listCategory } from '@/api/hm/category'
import { addService, delService, getService, listService, updateService } from '@/api/hm/service'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const serviceList = ref([])
const categoryOptions = ref([])
const areaOptions = ref([])
const ids = ref([])
const single = ref(true)

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    serviceName: undefined,
    categoryId: undefined,
    status: undefined
  },
  form: {},
  rules: {
    serviceName: [{ required: true, message: '服务名称不能为空', trigger: 'blur' }],
    categoryId: [{ required: true, message: '所属分类不能为空', trigger: 'change' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function emptyBookingRule() {
  return {
    advanceDays: 0,
    minAdvanceMinutes: 120,
    maxAdvanceDays: 7,
    allowSameDay: '1',
    timeSlotsJson: '["09:00-11:00","13:00-15:00"]',
    cancelRuleJson: '{"freeMinutes":120}',
    status: '0'
  }
}

function emptySku() {
  return {
    skuName: '',
    price: 0,
    originalPrice: 0,
    durationMinute: 0,
    sortNo: 0,
    status: '0'
  }
}

function emptyExtra() {
  return {
    extraName: '',
    extraPrice: 0,
    chargeType: 'once',
    sortNo: 0,
    status: '0'
  }
}

function emptyForm() {
  return {
    serviceItemId: undefined,
    categoryId: undefined,
    serviceName: '',
    orgId: 0,
    serviceSubTitle: '',
    serviceCover: '',
    serviceCoverStorage: 'local',
    serviceCoverObjectKey: '',
    serviceDesc: '',
    serviceContent: '',
    serviceDuration: 120,
    saleType: 'fixed',
    basePrice: 0,
    unitName: '次',
    needManualConfirm: '0',
    allowAssignWorker: '0',
    showInMiniapp: '1',
    showInPortal: '1',
    status: '0',
    remark: '',
    skuList: [],
    extraItemList: [],
    areaIdList: [],
    bookingRule: emptyBookingRule()
  }
}

function loadOptions() {
  listCategory({ pageNum: 1, pageSize: 1000 }).then(res => {
    categoryOptions.value = res.rows || []
  })
  listArea({ pageNum: 1, pageSize: 1000 }).then(res => {
    areaOptions.value = res.rows || []
  })
}

function getList() {
  loading.value = true
  listService(queryParams.value)
    .then(res => {
      serviceList.value = res.rows || []
      total.value = res.total || 0
    })
    .finally(() => {
      loading.value = false
    })
}

function reset() {
  form.value = emptyForm()
  proxy.resetForm('formRef')
}

function cancel() {
  open.value = false
  reset()
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.serviceItemId)
  single.value = selection.length !== 1
}

function handleAdd() {
  reset()
  title.value = '新增服务'
  open.value = true
}

function handleUpdate(row) {
  const serviceItemId = row?.serviceItemId || ids.value[0]
  if (!serviceItemId) {
    return
  }
  reset()
  getService(serviceItemId).then(res => {
    form.value = { ...emptyForm(), ...(res.data || {}) }
    form.value.skuList = form.value.skuList || []
    form.value.extraItemList = form.value.extraItemList || []
    form.value.areaIdList = form.value.areaIdList || []
    form.value.bookingRule = { ...emptyBookingRule(), ...(form.value.bookingRule || {}) }
    title.value = '修改服务'
    open.value = true
  })
}

function handleDelete(row) {
  const serviceItemId = row?.serviceItemId || ids.value[0]
  if (!serviceItemId) {
    return
  }
  proxy.$modal.confirm(`确认删除服务编号为 ${serviceItemId} 的数据项？`).then(() => {
    return delService(serviceItemId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  })
}

function addSku() {
  form.value.skuList.push(emptySku())
}

function removeSku(index) {
  form.value.skuList.splice(index, 1)
}

function addExtra() {
  form.value.extraItemList.push(emptyExtra())
}

function removeExtra(index) {
  form.value.extraItemList.splice(index, 1)
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) {
      return
    }
    const payload = JSON.parse(JSON.stringify(form.value))
    payload.skuList = payload.saleType === 'sku' ? payload.skuList.filter(item => item.skuName) : []
    payload.extraItemList = payload.extraItemList.filter(item => item.extraName)
    const action = payload.serviceItemId ? updateService : addService
    action(payload).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      open.value = false
      getList()
    })
  })
}

loadOptions()
getList()
</script>

<style scoped>
.mt8 {
  margin-top: 8px;
}
</style>
