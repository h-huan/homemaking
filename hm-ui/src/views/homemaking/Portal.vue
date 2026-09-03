<template>
  <HmPage
    eyebrow="HM · 官网内容"
    title="配置并发布租户官网"
    description="编辑导航、图片和首页模块，保存草稿后发布；未发布的修改不会出现在官网。"
  >
    <template #actions
      ><el-space
        ><el-button :loading="loading" @click="load">刷新</el-button
        ><el-button
          type="primary"
          :loading="publishing"
          :disabled="dirty || !version"
          @click="publish"
          >发布版本 {{ version }}</el-button
        ></el-space
      ></template
    >
    <el-alert
      v-if="dirty"
      title="内容尚未保存，保存后才能发布。"
      type="warning"
      :closable="false"
      show-icon
    />
    <el-alert
      v-else-if="publishedVersion"
      :title="'当前线上版本：' + publishedVersion"
      type="success"
      :closable="false"
    />
    <el-form v-if="config" v-loading="loading" label-position="top" class="portal-form">
      <el-divider content-position="left">搜索与公告</el-divider>
      <div class="grid"
        ><el-form-item label="页面标题"
          ><el-input v-model="config.seoTitle" maxlength="80" show-word-limit /></el-form-item
        ><el-form-item label="公告栏"
          ><el-input v-model="config.announcement" maxlength="120" /></el-form-item
      ></div>
      <el-form-item label="搜索摘要"
        ><el-input
          v-model="config.seoDescription"
          type="textarea"
          :rows="2"
          maxlength="300"
          show-word-limit
      /></el-form-item>
      <el-divider content-position="left">导航菜单</el-divider>
      <div v-for="(link, index) in config.navigation" :key="index" class="item-row">
        <el-input v-model="link.label" maxlength="40" placeholder="菜单名称" />
        <el-input
          v-model="link.href"
          maxlength="500"
          placeholder="#services、站内路径或 HTTPS 链接"
        />
        <el-button text type="danger" @click="config.navigation.splice(index, 1)">删除</el-button>
      </div>
      <el-button
        :disabled="config.navigation.length >= 8"
        text
        type="primary"
        @click="config.navigation.push({ label: '', href: '' })"
        >添加导航</el-button
      >
      <el-divider content-position="left">首屏主视觉</el-divider>
      <div class="grid"
        ><el-form-item label="主标题"
          ><el-input v-model="config.heroTitle" maxlength="80" /></el-form-item
        ><el-form-item label="主视觉图片地址"
          ><el-input
            v-model="config.heroImage"
            placeholder="HTTPS 图片或 /assets/..." /></el-form-item
      ></div>
      <img v-if="config.heroImage" :src="config.heroImage" class="hero-preview" alt="主视觉预览" />
      <el-form-item label="说明"
        ><el-input
          v-model="config.heroText"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
      /></el-form-item>
      <template v-for="key in actionKeys" :key="key">
        <div class="action-label"
          ><strong>{{ key === 'primaryAction' ? '主按钮' : '次按钮' }}</strong
          ><el-switch :model-value="!!config[key]" @change="toggleAction(key, !!$event)"
        /></div>
        <div v-if="config[key]" class="grid"
          ><el-form-item label="按钮文字"
            ><el-input v-model="config[key].label" maxlength="40" /></el-form-item
          ><el-form-item label="按钮链接"
            ><el-input
              v-model="config[key].href"
              placeholder="#services 或真实预约页面 HTTPS 地址"
              maxlength="500" /></el-form-item
        ></div>
      </template>
      <el-divider content-position="left">首页模块与展示顺序</el-divider>
      <el-collapse>
        <el-collapse-item
          v-for="(module, index) in config.modules"
          :key="module.type"
          :name="module.type"
        >
          <template #title
            ><el-switch v-model="module.enabled" @click.stop />
            <strong class="module-title"
              >{{ moduleName[module.type] || module.type }} · {{ module.title }}</strong
            ></template
          >
          <el-space class="module-actions"
            ><el-button
              :disabled="Number(index) === 0"
              size="small"
              @click="move(Number(index), -1)"
              >上移</el-button
            ><el-button
              :disabled="Number(index) === config.modules.length - 1"
              size="small"
              @click="move(Number(index), 1)"
              >下移</el-button
            ><el-button size="small" type="danger" plain @click="config.modules.splice(index, 1)"
              >删除模块</el-button
            ></el-space
          >
          <div class="grid"
            ><el-form-item label="标题"
              ><el-input v-model="module.title" maxlength="80" /></el-form-item
            ><el-form-item label="副标题"
              ><el-input v-model="module.subtitle" maxlength="300" /></el-form-item
          ></div>
          <el-form-item
            v-if="catalogTypes.includes(module.type)"
            label="指定展示内容（留空则展示最新可用内容）"
          >
            <el-select v-model="module.itemIds" multiple filterable placeholder="选择当前租户内容">
              <el-option
                v-for="item in choices[module.type] || []"
                :key="item.id"
                :value="item.id"
                :label="item.name || '评价 #' + item.id + ' · ' + item.content"
              />
            </el-select>
          </el-form-item>
          <template v-else>
            <div v-for="(item, itemIndex) in module.items" :key="itemIndex" class="content-item">
              <div class="item-row"
                ><el-input v-model="item.title" maxlength="80" placeholder="小标题" /><el-input
                  v-model="item.text"
                  maxlength="500"
                  placeholder="说明、联系方式或问题回答"
                /><el-button text type="danger" @click="module.items.splice(itemIndex, 1)"
                  >删除</el-button
                ></div
              >
              <el-input
                v-model="item.image"
                maxlength="1000"
                placeholder="内容图片地址（选填，HTTPS 或站内路径）"
              />
            </div>
            <el-button
              :disabled="module.items.length >= 12"
              text
              type="primary"
              @click="module.items.push({ title: '', text: '', image: '' })"
              >添加内容项</el-button
            >
          </template>
        </el-collapse-item>
      </el-collapse>
      <div class="add-module"
        ><el-select v-model="newModule" placeholder="选择要添加的模块"
          ><el-option
            v-for="(name, key) in moduleName"
            :key="key"
            :value="key"
            :label="name"
            :disabled="config.modules.some((m: any) => m.type === key)" /></el-select
        ><el-button @click="addModule">添加模块</el-button></div
      >
      <el-form-item label="页脚文字" class="footer-field"
        ><el-input v-model="config.footerText" maxlength="100"
      /></el-form-item>
      <el-button type="primary" :loading="saving" :disabled="!dirty" @click="save"
        >保存草稿</el-button
      >
    </el-form>
  </HmPage>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import * as api from '@/api/homemaking'
import HmPage from './components/HmPage.vue'
defineOptions({ name: 'HomemakingPortal' })
const loading = ref(false),
  saving = ref(false),
  publishing = ref(false),
  version = ref(0),
  publishedVersion = ref<number>()
const config = ref<any>(),
  saved = ref(''),
  newModule = ref(''),
  choices = ref<Record<string, any[]>>({})
const dirty = computed(() => !!config.value && JSON.stringify(config.value) !== saved.value)
const actionKeys = ['primaryAction', 'secondaryAction']
const catalogTypes = ['SERVICES', 'STORES', 'WORKERS', 'TESTIMONIALS']
const moduleName: Record<string, string> = {
  SERVICES: '服务项目',
  STORES: '服务网点',
  WORKERS: '服务团队',
  TRUST: '服务保障',
  PROCESS: '预约流程',
  TESTIMONIALS: '客户评价',
  ABOUT: '关于我们',
  CONTACT: '联系我们',
  FAQ: '常见问题'
}
async function load() {
  loading.value = true
  try {
    const data = await api.getPortal()
    config.value = data.config
    config.value.modules.forEach((m: any) => {
      m.items = m.items || []
      m.itemIds = m.itemIds || []
    })
    version.value = Number(data.version || 0)
    publishedVersion.value = data.publishedVersion
    saved.value = JSON.stringify(config.value)
  } finally {
    loading.value = false
  }
}
async function loadChoices() {
  const data = await Promise.all(
    ['services', 'stores', 'workers'].map((kind) => api.listCatalog(kind, { size: 100 }))
  )
  choices.value = {
    SERVICES: data[0].list,
    STORES: data[1].list,
    WORKERS: data[2].list,
    TESTIMONIALS: await api.listReviews()
  }
}
function move(index: number, offset: number) {
  const list = config.value.modules
  const item = list.splice(index, 1)[0]
  list.splice(index + offset, 0, item)
}
function addModule() {
  if (!newModule.value) return
  config.value.modules.push({
    type: newModule.value,
    title: moduleName[newModule.value],
    subtitle: '',
    enabled: true,
    itemIds: [],
    items: []
  })
  newModule.value = ''
}
function toggleAction(key: string, enabled: boolean) {
  config.value[key] = enabled
    ? {
        label: key === 'primaryAction' ? '立即预约' : '联系我们',
        href: key === 'primaryAction' ? '#services' : '#contact'
      }
    : null
}
async function save() {
  saving.value = true
  try {
    await api.savePortal({ config: config.value, version: version.value })
    await load()
    ElMessage.success('官网草稿已保存')
  } finally {
    saving.value = false
  }
}
async function publish() {
  publishing.value = true
  try {
    await api.publishPortal(version.value)
    await load()
    ElMessage.success('官网已发布')
  } finally {
    publishing.value = false
  }
}
onMounted(() => {
  load()
  loadChoices()
})
</script>
<style scoped>
.portal-form {
  max-width: 960px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 22px;
}
.module-title {
  margin-left: 12px;
}
.item-row {
  display: grid;
  grid-template-columns: 0.6fr 1.4fr auto;
  gap: 12px;
  margin-bottom: 12px;
}
.footer-field {
  margin-top: 30px;
}
.hero-preview {
  display: block;
  width: 100%;
  max-height: 280px;
  object-fit: cover;
  margin-bottom: 20px;
  border-radius: 8px;
}
.module-actions {
  margin: 10px 0 20px;
}
.action-label,
.add-module {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 20px 0;
}
.content-item {
  padding: 16px;
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin-bottom: 14px;
}
@media (max-width: 700px) {
  .grid,
  .item-row {
    grid-template-columns: 1fr;
  }
  .add-module {
    flex-wrap: wrap;
  }
}
</style>
