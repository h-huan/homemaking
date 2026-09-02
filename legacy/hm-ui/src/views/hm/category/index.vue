<template>
  <div class="app-container">
    <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
      <el-form-item label="分类名称" prop="categoryName">
        <el-input v-model="queryParams.categoryName" placeholder="请输入分类名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width: 160px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['hm:category:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['hm:category:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="single" @click="handleDelete()" v-hasPermi="['hm:category:edit']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="categoryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="分类ID" prop="categoryId" width="90" />
      <el-table-column label="分类名称" prop="categoryName" min-width="160" />
      <el-table-column label="上级ID" prop="parentId" width="100" />
      <el-table-column label="菜单图标" prop="categoryIcon" width="120" />
      <el-table-column label="排序" prop="sortNo" width="90" />
      <el-table-column label="小程序展示" width="110">
        <template #default="scope">{{ scope.row.showInMiniapp === '1' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="官网展示" width="110">
        <template #default="scope">{{ scope.row.showInPortal === '1' ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">{{ formatStatus(scope.row.status) }}</template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['hm:category:edit']">修改</el-button>
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['hm:category:edit']">删除</el-button>
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

    <el-dialog v-model="open" :title="title" width="680px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="上级ID">
          <el-input-number v-model="form.parentId" :min="0" />
        </el-form-item>
        <el-form-item label="系统图标">
          <el-select v-model="form.categoryIcon" placeholder="请选择菜单图标" style="width: 100%">
            <el-option v-for="item in iconOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="头图">
          <image-upload v-model="form.bannerImage" :limit="1" />
        </el-form-item>
        <el-form-item label="头图存储源">
          <el-select v-model="form.bannerImageStorage" style="width: 100%">
            <el-option label="本地" value="local" />
            <el-option label="阿里OSS" value="aliyun_oss" />
            <el-option label="腾讯COS" value="tencent_cos" />
            <el-option label="七牛云" value="qiniu" />
            <el-option label="其它OSS" value="other_oss" />
          </el-select>
        </el-form-item>
        <el-form-item label="头图对象键">
          <el-input v-model="form.bannerImageObjectKey" placeholder="如：category/banner/xxx.png" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="小程序展示">
          <el-radio-group v-model="form.showInMiniapp">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="官网展示">
          <el-radio-group v-model="form.showInPortal">
            <el-radio value="1">是</el-radio>
            <el-radio value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="HmCategory">
import { addCategory, delCategory, getCategory, listCategory, updateCategory } from '@/api/hm/category'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const open = ref(false)
const title = ref('')
const total = ref(0)
const categoryList = ref([])
const ids = ref([])
const single = ref(true)

const statusOptions = [
  { label: '启用', value: '0' },
  { label: '停用', value: '1' }
]

const iconOptions = [
  { label: '日常保洁', value: 'clean' },
  { label: '深度清洁', value: 'deep-clean' },
  { label: '家电清洗', value: 'kitchen' },
  { label: '维修安装', value: 'repair' },
  { label: '育儿看护', value: 'childcare' },
  { label: '宠物照护', value: 'pet' },
  { label: '地址服务', value: 'address' },
  { label: '默认图标', value: 'default' }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    categoryName: undefined,
    status: undefined
  },
  form: {},
  rules: {
    categoryName: [{ required: true, message: '分类名称不能为空', trigger: 'blur' }]
  }
})

const { queryParams, form, rules } = toRefs(data)

function emptyForm() {
  return {
    categoryId: undefined,
    parentId: 0,
    categoryName: '',
    categoryIcon: 'default',
    bannerImage: '',
    bannerImageStorage: 'local',
    bannerImageObjectKey: '',
    sortNo: 0,
    showInMiniapp: '1',
    showInPortal: '1',
    status: '0',
    remark: ''
  }
}

function formatStatus(value) {
  return value === '0' ? '启用' : '停用'
}

function getList() {
  loading.value = true
  listCategory(queryParams.value)
    .then(res => {
      categoryList.value = res.rows || []
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
  ids.value = selection.map(item => item.categoryId)
  single.value = selection.length !== 1
}

function handleAdd() {
  reset()
  open.value = true
  title.value = '新增分类'
}

function handleUpdate(row) {
  const categoryId = row?.categoryId || ids.value[0]
  if (!categoryId) {
    return
  }
  reset()
  getCategory(categoryId).then(res => {
    form.value = { ...emptyForm(), ...(res.data || {}) }
    open.value = true
    title.value = '修改分类'
  })
}

function handleDelete(row) {
  const categoryId = row?.categoryId || ids.value[0]
  if (!categoryId) {
    return
  }
  proxy.$modal.confirm(`确认删除分类编号为 ${categoryId} 的数据项？`).then(() => {
    return delCategory(categoryId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  })
}

function submitForm() {
  proxy.$refs.formRef.validate(valid => {
    if (!valid) {
      return
    }
    const action = form.value.categoryId ? updateCategory : addCategory
    action(form.value).then(() => {
      proxy.$modal.msgSuccess('保存成功')
      open.value = false
      getList()
    })
  })
}

getList()
</script>
