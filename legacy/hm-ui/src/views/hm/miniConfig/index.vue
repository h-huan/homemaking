<template>
  <div class="app-container">
    <el-alert
      title="页面框架由前端固定，首页分类、活动卡片、平台保障、我的入口等业务内容在这里配置后通过接口动态读取。"
      type="info"
      :closable="false"
      show-icon
    />

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>首页分类导航</span>
          <el-button type="primary" link @click="addServiceNav">新增导航</el-button>
        </div>
      </template>
      <el-table :data="form.serviceNavList" border>
        <el-table-column label="标题" min-width="140">
          <template #default="scope">
            <el-input v-model="scope.row.title" placeholder="如：家庭保洁" />
          </template>
        </el-table-column>
        <el-table-column label="关联分类" min-width="180">
          <template #default="scope">
            <el-select v-model="scope.row.categoryId" clearable filterable placeholder="请选择分类" style="width: 100%">
              <el-option
                v-for="item in categoryOptions"
                :key="item.categoryId"
                :label="item.categoryName"
                :value="item.categoryId"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="自定义路径" min-width="220">
          <template #default="scope">
            <el-input v-model="scope.row.path" placeholder="/pages/service/list/index?categoryId=1" />
          </template>
        </el-table-column>
        <el-table-column label="系统图标" min-width="180">
          <template #default="scope">
            <el-select v-model="scope.row.iconCode" clearable placeholder="请选择" style="width: 100%">
              <el-option v-for="item in iconOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="form.serviceNavList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>首页活动卡片</span>
          <el-button type="primary" link @click="addPromoCard">新增卡片</el-button>
        </div>
      </template>
      <el-table :data="form.promoCardList" border>
        <el-table-column label="标题" min-width="160">
          <template #default="scope">
            <el-input v-model="scope.row.title" placeholder="如：深度保洁" />
          </template>
        </el-table-column>
        <el-table-column label="副标题" min-width="220">
          <template #default="scope">
            <el-input v-model="scope.row.subTitle" placeholder="如：全屋到家清洁" />
          </template>
        </el-table-column>
        <el-table-column label="关联服务" min-width="220">
          <template #default="scope">
            <el-select v-model="scope.row.serviceItemId" clearable filterable placeholder="请选择服务" style="width: 100%">
              <el-option
                v-for="item in serviceOptions"
                :key="item.serviceItemId"
                :label="item.serviceName"
                :value="item.serviceItemId"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="图片" min-width="180">
          <template #default="scope">
            <image-upload v-model="scope.row.imageUrl" :limit="1" />
          </template>
        </el-table-column>
        <el-table-column label="存储源" width="130">
          <template #default="scope">
            <el-select v-model="scope.row.imageStorage" style="width: 100%">
              <el-option label="本地" value="local" />
              <el-option label="阿里OSS" value="aliyun_oss" />
              <el-option label="腾讯COS" value="tencent_cos" />
              <el-option label="七牛云" value="qiniu" />
              <el-option label="其它OSS" value="other_oss" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="对象键" min-width="180">
          <template #default="scope">
            <el-input v-model="scope.row.imageObjectKey" placeholder="promo/xxx.png" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="form.promoCardList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>分类页头图</span>
          <el-button type="primary" link @click="addCategoryBanner">新增头图</el-button>
        </div>
      </template>
      <el-table :data="form.categoryBannerList" border>
        <el-table-column label="标题" min-width="160">
          <template #default="scope">
            <el-input v-model="scope.row.title" />
          </template>
        </el-table-column>
        <el-table-column label="副标题" min-width="220">
          <template #default="scope">
            <el-input v-model="scope.row.subTitle" />
          </template>
        </el-table-column>
        <el-table-column label="图片" min-width="180">
          <template #default="scope">
            <image-upload v-model="scope.row.imageUrl" :limit="1" />
          </template>
        </el-table-column>
        <el-table-column label="存储源" width="130">
          <template #default="scope">
            <el-select v-model="scope.row.imageStorage" style="width: 100%">
              <el-option label="本地" value="local" />
              <el-option label="阿里OSS" value="aliyun_oss" />
              <el-option label="腾讯COS" value="tencent_cos" />
              <el-option label="七牛云" value="qiniu" />
              <el-option label="其它OSS" value="other_oss" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="对象键" min-width="180">
          <template #default="scope">
            <el-input v-model="scope.row.imageObjectKey" placeholder="category/banner/xxx.png" />
          </template>
        </el-table-column>
        <el-table-column label="关联分类" min-width="180">
          <template #default="scope">
            <el-select v-model="scope.row.categoryId" clearable filterable style="width: 100%">
              <el-option v-for="item in categoryOptions" :key="item.categoryId" :label="item.categoryName" :value="item.categoryId" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button link type="danger" @click="form.categoryBannerList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>分类页分组</span>
          <el-button type="primary" link @click="addCategorySection">新增分组</el-button>
        </div>
      </template>
      <el-table :data="form.categorySectionList" border>
        <el-table-column label="标题" min-width="180">
          <template #default="scope">
            <el-input v-model="scope.row.title" />
          </template>
        </el-table-column>
        <el-table-column label="起始位置" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.start" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="数量" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.limit" :min="1" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="120">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button link type="danger" @click="form.categorySectionList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>平台保障</span>
          <el-button type="primary" link @click="addAdvantage">新增保障项</el-button>
        </div>
      </template>
      <el-table :data="form.advantageList" border>
        <el-table-column label="保障文案" min-width="260">
          <template #default="scope">
            <el-input v-model="scope.row.text" placeholder="如：实名认证" />
          </template>
        </el-table-column>
        <el-table-column label="图标上传" min-width="180">
          <template #default="scope">
            <image-upload v-model="scope.row.iconUrl" :limit="1" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button link type="danger" @click="form.advantageList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>“我的”页入口</span>
          <el-button type="primary" link @click="addMineMenu">新增入口</el-button>
        </div>
      </template>
      <el-table :data="form.mineMenuList" border>
        <el-table-column label="标题" min-width="140">
          <template #default="scope">
            <el-input v-model="scope.row.title" placeholder="如：地址管理" />
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="200">
          <template #default="scope">
            <el-input v-model="scope.row.desc" placeholder="如：维护服务地址" />
          </template>
        </el-table-column>
        <el-table-column label="系统图标" min-width="180">
          <template #default="scope">
            <el-select v-model="scope.row.iconCode" clearable placeholder="请选择" style="width: 100%">
              <el-option v-for="item in iconOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="分组" width="120">
          <template #default="scope">
            <el-select v-model="scope.row.group" placeholder="请选择">
              <el-option label="服务入口" value="service" />
              <el-option label="内容服务" value="content" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="scope">
            <el-select v-model="scope.row.entryType" placeholder="请选择">
              <el-option label="页面跳转" value="path" />
              <el-option label="内容页" value="content" />
              <el-option label="联系客服" value="action" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="页面路径" min-width="220">
          <template #default="scope">
            <el-input v-model="scope.row.path" :disabled="scope.row.entryType !== 'path'" placeholder="/pages/address/list/index" />
          </template>
        </el-table-column>
        <el-table-column label="内容类型" min-width="160">
          <template #default="scope">
            <el-select
              v-model="scope.row.contentType"
              :disabled="scope.row.entryType !== 'content'"
              clearable
              placeholder="请选择"
              style="width: 100%"
            >
              <el-option
                v-for="item in contentList"
                :key="item.contentId"
                :label="`${item.title}（${item.contentType}）`"
                :value="item.contentType"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作动作" width="140">
          <template #default="scope">
            <el-select v-model="scope.row.action" :disabled="scope.row.entryType !== 'action'" clearable placeholder="请选择">
              <el-option label="联系客服" value="contact" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="form.mineMenuList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>搜索区配置</span>
        </div>
      </template>
      <el-form :model="form.searchConfig" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="城市名称">
              <el-input v-model="form.searchConfig.cityName" placeholder="如：深圳市" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="搜索提示">
              <el-input v-model="form.searchConfig.placeholder" placeholder="如：搜索服务、项目关键词" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="默认关键词">
              <el-input v-model="form.searchConfig.keyword" placeholder="如：深度保洁" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>首页区块标题</span>
        </div>
      </template>
      <el-form :model="form.sectionConfig" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="家务全家桶">
              <el-input v-model="form.sectionConfig.familyTitle" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="家务更多文案">
              <el-input v-model="form.sectionConfig.familyMoreText" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="热门推荐">
              <el-input v-model="form.sectionConfig.recommendTitle" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="推荐更多文案">
              <el-input v-model="form.sectionConfig.recommendMoreText" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="平台保障">
              <el-input v-model="form.sectionConfig.advantageTitle" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="快捷入口">
              <el-input v-model="form.sectionConfig.quickTitle" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>联系方式</span>
        </div>
      </template>
      <el-form :model="form.contactConfig" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客服电话">
              <el-input v-model="form.contactConfig.phone" placeholder="如：400-000-0000" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客服微信">
              <el-input v-model="form.contactConfig.wechat" placeholder="如：hm-service" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>小程序导航配置</span>
          <el-button type="primary" link @click="addNavRow">新增导航</el-button>
        </div>
      </template>
      <el-table :data="navConfigList" border>
        <el-table-column label="名称" min-width="140">
          <template #default="scope">
            <el-input v-model="scope.row.navName" placeholder="如：保洁收纳" />
          </template>
        </el-table-column>
        <el-table-column label="图标" min-width="140">
          <template #default="scope">
            <el-input v-model="scope.row.navIcon" placeholder="预留图标字段" />
          </template>
        </el-table-column>
        <el-table-column label="跳转路径" min-width="220">
          <template #default="scope">
            <el-input v-model="scope.row.navPath" placeholder="/pages/service/list/index?categoryId=1" />
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100">
          <template #default="scope">
            <el-input-number v-model="scope.row.sortNo" :min="0" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-select v-model="scope.row.status">
              <el-option label="启用" value="0" />
              <el-option label="停用" value="1" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="scope">
            <el-button link type="danger" @click="navConfigList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="mt16">
      <template #header>
        <div class="card-header">
          <span>内容页配置</span>
          <el-button type="primary" link @click="handleAddContent">新增内容</el-button>
        </div>
      </template>
      <el-table :data="contentList" border>
        <el-table-column label="内容标识" min-width="180" prop="contentType" />
        <el-table-column label="标题" min-width="180" prop="title" />
        <el-table-column label="副标题" min-width="180" prop="subTitle" />
        <el-table-column label="发布状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.publishStatus === '1' ? 'success' : 'info'">
              {{ scope.row.publishStatus === '1' ? '已发布' : '未发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="100" prop="sortNo" />
        <el-table-column label="更新时间" min-width="180" prop="updateTime" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="handleEditContent(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="handleDeleteContent(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="action-bar">
      <el-button type="primary" @click="submitAll">保存运营配置</el-button>
      <el-button @click="loadData">重新加载</el-button>
    </div>

    <el-dialog v-model="contentDialog.visible" :title="contentDialog.title" width="760px">
      <el-form :model="contentForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="内容标识">
              <el-input v-model="contentForm.contentType" placeholder="如：privacy_policy" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布状态">
              <el-select v-model="contentForm.publishStatus" style="width: 100%">
                <el-option label="未发布" value="0" />
                <el-option label="已发布" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="标题">
              <el-input v-model="contentForm.title" placeholder="请输入标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="副标题">
              <el-input v-model="contentForm.subTitle" placeholder="请输入副标题" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图">
          <image-upload v-model="contentForm.coverImage" :limit="1" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="图片存储源">
              <el-select v-model="contentForm.coverImageStorage" style="width: 100%">
                <el-option label="本地" value="local" />
                <el-option label="阿里OSS" value="aliyun_oss" />
                <el-option label="腾讯COS" value="tencent_cos" />
                <el-option label="七牛云" value="qiniu" />
                <el-option label="其它OSS" value="other_oss" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="对象键">
              <el-input v-model="contentForm.coverImageObjectKey" placeholder="content/cover/xxx.png" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="摘要">
          <el-input v-model="contentForm.summary" type="textarea" :rows="3" placeholder="请输入摘要" />
        </el-form-item>
        <el-form-item label="正文内容">
          <el-input v-model="contentForm.contentHtml" type="textarea" :rows="12" placeholder="请输入 HTML 内容" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="contentForm.sortNo" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contentDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitContent">保存内容</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="HmMiniConfig">
import { listCategory } from '@/api/hm/category'
import { listService } from '@/api/hm/service'
import {
  deletePortalContent,
  getPortalContent,
  listHomeConfig,
  listNavConfig,
  listPortalContent,
  saveHomeConfig,
  saveNavConfig,
  savePortalContent
} from '@/api/hm/miniConfig'

const { proxy } = getCurrentInstance()

const categoryOptions = ref([])
const serviceOptions = ref([])
const navConfigList = ref([])
const contentList = ref([])
const rawHomeConfigMap = ref({})
const iconOptions = [
  { label: '日常保洁', value: 'clean' },
  { label: '深度清洁', value: 'deep-clean' },
  { label: '厨房家电', value: 'kitchen' },
  { label: '维修安装', value: 'repair' },
  { label: '做饭育儿', value: 'childcare' },
  { label: '宠物照护', value: 'pet' },
  { label: '订单', value: 'order' },
  { label: '日程', value: 'schedule' },
  { label: '地址', value: 'address' },
  { label: '客服', value: 'contact' },
  { label: '隐私', value: 'privacy' },
  { label: '说明', value: 'guide' },
  { label: '关于我们', value: 'about' },
  { label: '默认', value: 'default' }
]
const contentDialog = reactive({
  visible: false,
  title: '新增内容'
})
const contentForm = reactive({
  contentId: undefined,
  contentType: '',
  title: '',
  subTitle: '',
  coverImage: '',
  coverImageStorage: 'local',
  coverImageObjectKey: '',
  summary: '',
  contentHtml: '',
  sortNo: 0,
  publishStatus: '0'
})
const form = reactive({
  serviceNavList: [],
  promoCardList: [],
  categoryBannerList: [],
  categorySectionList: [],
  advantageList: [],
  mineMenuList: [],
  searchConfig: {
    cityName: '',
    placeholder: '',
    keyword: ''
  },
  sectionConfig: {
    familyTitle: '',
    familyMoreText: '',
    recommendTitle: '',
    recommendMoreText: '',
    advantageTitle: '',
    quickTitle: ''
  },
  contactConfig: {
    phone: '',
    wechat: ''
  }
})

function parseJson(value, fallback) {
  if (!value) {
    return fallback
  }
  try {
    return JSON.parse(value)
  } catch (error) {
    return fallback
  }
}

function sortBySortNo(list) {
  return [...(list || [])].sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0))
}

function createServiceNavRow(data = {}) {
  return {
    title: data.title || '',
    categoryId: data.categoryId || data.id || undefined,
    path: data.path || data.navPath || '',
    iconCode: data.iconCode || data.iconUrl || data.icon || data.navIcon || '',
    sortNo: data.sortNo || 0
  }
}

function createPromoCardRow(data = {}) {
  return {
    title: data.title || '',
    subTitle: data.subTitle || '',
    serviceItemId: data.serviceItemId || undefined,
    imageUrl: data.imageUrl || data.coverImage || '',
    imageStorage: data.imageStorage || 'local',
    imageObjectKey: data.imageObjectKey || '',
    sortNo: data.sortNo || 0
  }
}

function createAdvantageRow(data = {}) {
  return {
    text: typeof data === 'string' ? data : (data.text || ''),
    iconUrl: typeof data === 'string' ? '' : (data.iconUrl || ''),
    sortNo: typeof data === 'string' ? 0 : (data.sortNo || 0)
  }
}

function createCategoryBannerRow(data = {}) {
  return {
    title: data.title || '',
    subTitle: data.subTitle || '',
    imageUrl: data.imageUrl || data.coverImage || '',
    imageStorage: data.imageStorage || 'local',
    imageObjectKey: data.imageObjectKey || '',
    categoryId: data.categoryId || undefined,
    sortNo: data.sortNo || 0
  }
}

function createCategorySectionRow(data = {}) {
  return {
    title: data.title || '',
    start: data.start || 0,
    limit: data.limit || 4,
    sortNo: data.sortNo || 0
  }
}

function createMineMenuRow(data = {}) {
  const entryType = data.contentType ? 'content' : (data.action ? 'action' : 'path')
  return {
    key: data.key || '',
    title: data.title || '',
    desc: data.desc || '',
    iconCode: data.iconCode || data.iconUrl || '',
    group: data.group || 'service',
    entryType,
    path: data.path || '',
    contentType: data.contentType || '',
    action: data.action || '',
    sortNo: data.sortNo || 0
  }
}

function resetContentForm() {
  contentForm.contentId = undefined
  contentForm.contentType = ''
  contentForm.title = ''
  contentForm.subTitle = ''
  contentForm.coverImage = ''
  contentForm.coverImageStorage = 'local'
  contentForm.coverImageObjectKey = ''
  contentForm.summary = ''
  contentForm.contentHtml = ''
  contentForm.sortNo = 0
  contentForm.publishStatus = '0'
}

function resetForm() {
  form.serviceNavList = []
  form.promoCardList = []
  form.categoryBannerList = []
  form.categorySectionList = []
  form.advantageList = []
  form.mineMenuList = []
  form.searchConfig = {
    cityName: '',
    placeholder: '',
    keyword: ''
  }
  form.sectionConfig = {
    familyTitle: '',
    familyMoreText: '',
    recommendTitle: '',
    recommendMoreText: '',
    advantageTitle: '',
    quickTitle: ''
  }
  form.contactConfig = {
    phone: '',
    wechat: ''
  }
}

function applyHomeConfigs(configMap) {
  resetForm()
  form.serviceNavList = sortBySortNo(parseJson(configMap.serviceNavList, []).map(createServiceNavRow))
  form.promoCardList = sortBySortNo(parseJson(configMap.promoCardList, []).map(createPromoCardRow))
  form.categoryBannerList = sortBySortNo(parseJson(configMap.categoryBannerList, []).map(createCategoryBannerRow))
  form.categorySectionList = sortBySortNo(parseJson(configMap.categorySectionList, []).map(createCategorySectionRow))
  form.advantageList = sortBySortNo(parseJson(configMap.advantageList, []).map(createAdvantageRow))
  form.mineMenuList = sortBySortNo(parseJson(configMap.mineMenuList, []).map(createMineMenuRow))
  form.searchConfig = {
    cityName: parseJson(configMap.searchConfig, {}).cityName || '',
    placeholder: parseJson(configMap.searchConfig, {}).placeholder || '',
    keyword: parseJson(configMap.searchConfig, {}).keyword || ''
  }
  form.sectionConfig = {
    familyTitle: parseJson(configMap.sectionConfig, {}).familyTitle || '',
    familyMoreText: parseJson(configMap.sectionConfig, {}).familyMoreText || '',
    recommendTitle: parseJson(configMap.sectionConfig, {}).recommendTitle || '',
    recommendMoreText: parseJson(configMap.sectionConfig, {}).recommendMoreText || '',
    advantageTitle: parseJson(configMap.sectionConfig, {}).advantageTitle || '',
    quickTitle: parseJson(configMap.sectionConfig, {}).quickTitle || ''
  }
  form.contactConfig = {
    phone: parseJson(configMap.contactConfig, {}).phone || '',
    wechat: parseJson(configMap.contactConfig, {}).wechat || ''
  }
}

function addServiceNav() {
  form.serviceNavList.push(createServiceNavRow())
}

function addPromoCard() {
  form.promoCardList.push(createPromoCardRow())
}

function addAdvantage() {
  form.advantageList.push(createAdvantageRow())
}

function addCategoryBanner() {
  form.categoryBannerList.push(createCategoryBannerRow())
}

function addCategorySection() {
  form.categorySectionList.push(createCategorySectionRow())
}

function addMineMenu() {
  form.mineMenuList.push(createMineMenuRow())
}

function addNavRow() {
  navConfigList.value.push({
    orgId: 0,
    navName: '',
    navIcon: '',
    navPath: '',
    sortNo: 0,
    status: '0'
  })
}

function handleAddContent() {
  resetContentForm()
  contentDialog.title = '新增内容'
  contentDialog.visible = true
}

async function handleEditContent(row) {
  const res = await getPortalContent(row.contentId)
  const data = res.data || {}
  contentForm.contentId = data.contentId
  contentForm.contentType = data.contentType || ''
  contentForm.title = data.title || ''
  contentForm.subTitle = data.subTitle || ''
  contentForm.coverImage = data.coverImage || ''
  contentForm.coverImageStorage = data.coverImageStorage || 'local'
  contentForm.coverImageObjectKey = data.coverImageObjectKey || ''
  contentForm.summary = data.summary || ''
  contentForm.contentHtml = data.contentHtml || ''
  contentForm.sortNo = data.sortNo || 0
  contentForm.publishStatus = data.publishStatus || '0'
  contentDialog.title = '编辑内容'
  contentDialog.visible = true
}

async function handleDeleteContent(row) {
  await proxy.$modal.confirm(`确认删除内容“${row.title || row.contentType}”吗？`)
  await deletePortalContent(row.contentId)
  proxy.$modal.msgSuccess('删除成功')
  await loadPortalContents()
}

async function submitContent() {
  if (!contentForm.contentType || !contentForm.title) {
    proxy.$modal.msgError('请先填写内容标识和标题')
    return
  }
  await savePortalContent({ ...contentForm })
  proxy.$modal.msgSuccess('内容保存成功')
  contentDialog.visible = false
  await loadPortalContents()
}

function buildHomeConfigPayload() {
  const preservedConfigs = Object.values(rawHomeConfigMap.value).filter(
    item => !['serviceNavList', 'promoCardList', 'categoryBannerList', 'categorySectionList', 'advantageList', 'mineMenuList', 'searchConfig', 'sectionConfig', 'contactConfig'].includes(item.configKey)
  )
  const defaults = {
    orgId: 0,
    status: '0'
  }
  const managedConfigs = [
    {
      ...(rawHomeConfigMap.value.serviceNavList || defaults),
      configKey: 'serviceNavList',
      configValue: JSON.stringify(
        sortBySortNo(form.serviceNavList)
          .filter(item => item.title)
          .map(item => ({
            title: item.title,
            categoryId: item.categoryId || null,
            id: item.categoryId || null,
            path: item.path || '',
            iconCode: item.iconCode || '',
            sortNo: item.sortNo || 0
          }))
      )
    },
    {
      ...(rawHomeConfigMap.value.promoCardList || defaults),
      configKey: 'promoCardList',
      configValue: JSON.stringify(
        sortBySortNo(form.promoCardList)
          .filter(item => item.title)
          .map(item => ({
            title: item.title,
            subTitle: item.subTitle || '',
            serviceItemId: item.serviceItemId || null,
            imageUrl: item.imageUrl || '',
            imageStorage: item.imageStorage || 'local',
            imageObjectKey: item.imageObjectKey || '',
            sortNo: item.sortNo || 0
          }))
      )
    },
    {
      ...(rawHomeConfigMap.value.categoryBannerList || defaults),
      configKey: 'categoryBannerList',
      configValue: JSON.stringify(
        sortBySortNo(form.categoryBannerList)
          .filter(item => item.title || item.imageUrl)
          .map(item => ({
            title: item.title || '',
            subTitle: item.subTitle || '',
            imageUrl: item.imageUrl || '',
            imageStorage: item.imageStorage || 'local',
            imageObjectKey: item.imageObjectKey || '',
            categoryId: item.categoryId || null,
            sortNo: item.sortNo || 0
          }))
      )
    },
    {
      ...(rawHomeConfigMap.value.categorySectionList || defaults),
      configKey: 'categorySectionList',
      configValue: JSON.stringify(
        sortBySortNo(form.categorySectionList)
          .filter(item => item.title)
          .map(item => ({
            title: item.title,
            start: item.start || 0,
            limit: item.limit || 4,
            sortNo: item.sortNo || 0
          }))
      )
    },
    {
      ...(rawHomeConfigMap.value.advantageList || defaults),
      configKey: 'advantageList',
      configValue: JSON.stringify(
        sortBySortNo(form.advantageList)
          .filter(item => item.text)
          .map(item => ({
            text: item.text,
            iconUrl: item.iconUrl || '',
            sortNo: item.sortNo || 0
          }))
      )
    },
    {
      ...(rawHomeConfigMap.value.mineMenuList || defaults),
      configKey: 'mineMenuList',
      configValue: JSON.stringify(
        sortBySortNo(form.mineMenuList)
          .filter(item => item.title)
          .map((item, index) => ({
            key: item.key || `menu_${index + 1}`,
            title: item.title,
            desc: item.desc || '',
            iconCode: item.iconCode || '',
            group: item.group || 'service',
            path: item.entryType === 'path' ? (item.path || '') : '',
            contentType: item.entryType === 'content' ? (item.contentType || '') : '',
            action: item.entryType === 'action' ? (item.action || '') : '',
            sortNo: item.sortNo || 0
          }))
      )
    },
    {
      ...(rawHomeConfigMap.value.searchConfig || defaults),
      configKey: 'searchConfig',
      configValue: JSON.stringify({
        cityName: form.searchConfig.cityName || '',
        placeholder: form.searchConfig.placeholder || '',
        keyword: form.searchConfig.keyword || ''
      })
    },
    {
      ...(rawHomeConfigMap.value.sectionConfig || defaults),
      configKey: 'sectionConfig',
      configValue: JSON.stringify({
        familyTitle: form.sectionConfig.familyTitle || '',
        familyMoreText: form.sectionConfig.familyMoreText || '',
        recommendTitle: form.sectionConfig.recommendTitle || '',
        recommendMoreText: form.sectionConfig.recommendMoreText || '',
        advantageTitle: form.sectionConfig.advantageTitle || '',
        quickTitle: form.sectionConfig.quickTitle || ''
      })
    },
    {
      ...(rawHomeConfigMap.value.contactConfig || defaults),
      configKey: 'contactConfig',
      configValue: JSON.stringify({
        phone: form.contactConfig.phone || '',
        wechat: form.contactConfig.wechat || ''
      })
    }
  ]
  return [...preservedConfigs, ...managedConfigs]
}

async function loadOptions() {
  const [categoryRes, serviceRes] = await Promise.all([
    listCategory({ pageNum: 1, pageSize: 1000 }),
    listService({ pageNum: 1, pageSize: 1000 })
  ])
  categoryOptions.value = categoryRes.rows || []
  serviceOptions.value = serviceRes.rows || []
}

async function loadData() {
  const [homeRes, navRes] = await Promise.all([listHomeConfig(), listNavConfig()])
  const homeConfigList = (homeRes.data || []).map(item => ({ orgId: 0, status: '0', ...item }))
  rawHomeConfigMap.value = homeConfigList.reduce((acc, item) => {
    acc[item.configKey] = item
    return acc
  }, {})
  applyHomeConfigs(rawHomeConfigMap.value)
  navConfigList.value = (navRes.data || []).map(item => ({ orgId: 0, status: '0', ...item }))
}

async function loadPortalContents() {
  const res = await listPortalContent()
  contentList.value = res.data || []
}

async function submitAll() {
  const homePayload = buildHomeConfigPayload()
  const navPayload = navConfigList.value.filter(item => item.navName)
  await Promise.all([saveHomeConfig(homePayload), saveNavConfig(navPayload)])
  proxy.$modal.msgSuccess('运营配置保存成功')
  loadData()
}

onMounted(async () => {
  await loadOptions()
  await loadData()
  await loadPortalContents()
})
</script>

<style scoped>
.mt16 {
  margin-top: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 600;
}

.action-bar {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}
</style>
