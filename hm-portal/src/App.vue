<template>
  <div v-if="loading" class="state-screen"><span class="loader" />正在准备服务内容…</div>
  <main v-else-if="error" class="state-screen"><strong>官网暂时无法访问</strong><p>{{ error }}</p><button @click="load">重新加载</button></main>
  <div v-else-if="site" class="site" :style="{ '--brand': site.brand.primary_color || '#174f43' }">
    <a class="skip-link" href="#content">跳到主要内容</a>
    <div v-if="site.config.announcement" class="announcement">{{ site.config.announcement }}</div>
    <header class="header shell">
      <a class="brand" href="#top" aria-label="返回首页">
        <img v-if="site.brand.logo" :src="site.brand.logo" alt="" /><span class="brand-mark" v-else>HM</span>
        <span>{{ site.brand.brand_name || site.brand.tenant_name || 'HM 家政' }}</span>
      </a>
      <button class="menu-toggle" :aria-expanded="menuOpen" aria-controls="main-navigation" @click="menuOpen=!menuOpen">{{ menuOpen ? '关闭菜单' : '菜单' }} <span aria-hidden="true">☰</span></button>
      <nav id="main-navigation" :class="{open:menuOpen}" aria-label="主导航"><a v-for="link in site.config.navigation" :key="link.href" :href="link.href" @click="menuOpen=false">{{ link.label }}</a></nav>
      <a class="header-action" :href="site.config.primaryAction?.href || '#services'">预约服务</a>
    </header>

    <section id="top" class="hero">
      <div class="hero-photo" :style="{ backgroundImage: `url('${site.config.heroImage || '/assets/hm-hero-home.png'}')` }" />
      <div class="shell hero-content">
        <p class="eyebrow">专业 · 透明 · 可追踪</p>
        <h1>{{ site.config.heroTitle }}</h1>
        <p class="hero-copy">{{ site.config.heroText }}</p>
        <div class="hero-actions">
          <a v-if="site.config.primaryAction" class="button primary" :href="site.config.primaryAction.href">{{ site.config.primaryAction.label }}</a>
          <a v-if="site.config.secondaryAction" class="button secondary" :href="site.config.secondaryAction.href">{{ site.config.secondaryAction.label }}</a>
        </div>
        <div class="confidence"><span>实名服务</span><span>价格透明</span><span>售后可查</span></div>
      </div>
    </section>

    <div id="content">
      <template v-for="(module, index) in enabledModules" :key="`${module.type}-${index}`">
        <section v-if="module.type === 'SERVICES'" id="services" class="section shell">
          <SectionHeading :title="module.title" :subtitle="module.subtitle" eyebrow="按需预约" />
          <div class="service-filters" aria-label="服务分类"><button v-for="category in categories" :key="category" :class="{active:selectedCategory===category}" :aria-pressed="selectedCategory===category" @click="selectedCategory=category">{{ category }}</button></div>
          <div class="service-grid">
            <article v-for="service in filteredServices" :key="service.id" class="service-card">
              <div class="service-image" :style="service.cover ? { backgroundImage: `url('${service.cover}')` } : {}"><span v-if="!service.cover">{{ service.category || '到家服务' }}</span></div>
              <div class="service-body"><small>{{ service.category }}</small><h3>{{ service.name }}</h3><p>{{ service.description || '标准服务流程，预约后可随时查看进度。' }}</p><div><strong>¥{{ money(service.price_cents) }}</strong><span> / {{ service.duration_minutes }}分钟</span></div><button class="text-button" @click="openService(service)">查看服务 <span aria-hidden="true">↗</span></button></div>
            </article>
          </div>
          <p v-if="!filteredServices.length" class="empty-state">服务项目正在准备中，欢迎联系门店咨询。</p>
        </section>

        <section v-else-if="module.type === 'TRUST' || module.type === 'PROCESS'" :id="module.type.toLowerCase()" class="section band">
          <div class="shell"><SectionHeading :title="module.title" :subtitle="module.subtitle" :eyebrow="module.type === 'TRUST' ? '服务保障' : '预约流程'" />
            <div class="feature-line"><article v-for="(item, i) in module.items" :key="item.title"><span>{{ String(i + 1).padStart(2, '0') }}</span><img v-if="item.image" :src="item.image" :alt="item.title" loading="lazy"/><h3>{{ item.title }}</h3><p>{{ item.text }}</p></article></div>
          </div>
        </section>

        <section v-else-if="module.type === 'STORES'" id="stores" class="section shell">
          <SectionHeading :title="module.title" :subtitle="module.subtitle" eyebrow="服务网点" />
          <div class="list-lines"><article v-for="store in site.catalog.stores" :key="store.id"><div><h3>{{ store.name }}</h3><p>{{ store.address }}</p><a v-if="telephone(store.phone)" :href="telephone(store.phone)">{{ store.phone }}</a></div><span>{{ store.service_area || '本地服务' }}</span></article></div>
        </section>

        <section v-else-if="module.type === 'WORKERS'" id="workers" class="section shell">
          <SectionHeading :title="module.title" :subtitle="module.subtitle" eyebrow="服务团队" />
          <div class="people"><article v-for="worker in site.catalog.workers" :key="worker.id"><img v-if="worker.avatar" :src="worker.avatar" alt="" /><div class="avatar" v-else>{{ String(worker.name).slice(0, 1) }}</div><h3>{{ worker.name }}</h3><p>{{ worker.skills }}</p></article></div>
        </section>

        <section v-else-if="module.type === 'TESTIMONIALS'" id="reviews" class="section reviews">
          <div class="shell"><SectionHeading :title="module.title" :subtitle="module.subtitle" eyebrow="真实评价" />
            <div class="quotes"><blockquote v-for="review in site.catalog.reviews" :key="review.id"><div aria-label="评分">{{ '★'.repeat(review.rating) }}</div><p>“{{ review.content }}”</p><footer>{{ review.nickname || '客户' }}</footer></blockquote></div>
          </div>
        </section>

        <section v-else-if="module.type === 'ABOUT'" id="about" class="section statement shell"><p class="eyebrow">关于我们</p><h2>{{ module.title }}</h2><p>{{ module.subtitle }}</p><ContentItems :items="module.items" /></section>

        <section v-else-if="module.type === 'CONTACT'" id="contact" class="contact shell"><div><p class="eyebrow">服务咨询</p><h2>{{ module.title }}</h2><p>{{ module.subtitle }}</p><ContentItems :items="module.items"/><div v-for="store in site.catalog.stores" :key="store.id" class="contact-store"><strong>{{ store.name }}</strong><a v-if="telephone(store.phone)" :href="telephone(store.phone)">{{ store.phone }}</a></div></div><a v-if="bookingLink" class="button light" :href="bookingLink">前往预约</a></section>

        <section v-else-if="module.type === 'FAQ'" id="faq" class="section shell"><SectionHeading :title="module.title" :subtitle="module.subtitle" eyebrow="常见问题" /><details v-for="item in module.items" :key="item.title"><summary>{{ item.title }}</summary><p>{{ item.text }}</p></details></section>
      </template>
    </div>
    <footer class="footer shell"><div class="brand"><span class="brand-mark">HM</span><span>{{ site.brand.brand_name || 'HM 家政' }}</span></div><p>{{ site.config.footerText }}</p></footer>
    <dialog ref="serviceDialog" class="service-dialog" @click="closeOutside" @close="selectedService=undefined">
      <template v-if="selectedService"><button class="dialog-close" aria-label="关闭服务详情" @click="serviceDialog?.close()">×</button><p class="eyebrow">{{ selectedService.category }}</p><h2>{{ selectedService.name }}</h2><img v-if="selectedService.cover" :src="selectedService.cover" :alt="selectedService.name"/><p class="service-description">{{ selectedService.description }}</p><strong>¥{{ money(selectedService.price_cents) }} · {{ selectedService.duration_minutes }}分钟</strong><p>具体时段与最终价格以预约确认页为准。</p><a v-if="bookingLink" class="button primary" :href="bookingLink">前往预约</a><a v-else class="button primary" href="#contact" @click="serviceDialog?.close()">联系门店预约</a></template>
    </dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import type { PortalData } from './types'
import SectionHeading from './components/SectionHeading.vue'
import ContentItems from './components/ContentItems.vue'

const site = ref<PortalData>(); const loading = ref(true); const error = ref('')
const enabledModules = computed(() => site.value?.config.modules.filter((item) => item.enabled) || [])
const menuOpen=ref(false),selectedCategory=ref('全部'),selectedService=ref<Record<string,any>>(),serviceDialog=ref<HTMLDialogElement>()
const categories=computed(()=>['全部',...new Set((site.value?.catalog.services||[]).map(s=>String(s.category||'到家服务')))])
const filteredServices=computed(()=>(site.value?.catalog.services||[]).filter(s=>selectedCategory.value==='全部'||(s.category||'到家服务')===selectedCategory.value))
const bookingLink=computed(()=>{const href=site.value?.config.primaryAction?.href;return href&&!href.startsWith('#')?href:''})
const telephone=(phone:unknown)=>typeof phone==='string'&&/^[+0-9 ()-]{3,32}$/.test(phone)?'tel:'+phone.replace(/[ ()-]/g,''):''
const money = (cents: number) => (Number(cents || 0) / 100).toFixed(2)
async function openService(service:Record<string,any>){selectedService.value=service;await nextTick();serviceDialog.value?.showModal()}
function closeOutside(event:MouseEvent){if(event.target===serviceDialog.value){const rect=serviceDialog.value.getBoundingClientRect();if(event.clientX<rect.left||event.clientX>rect.right||event.clientY<rect.top||event.clientY>rect.bottom)serviceDialog.value.close()}}
async function load() { loading.value = true; error.value = ''; try { const response = await fetch('/app-api/homemaking/public/portal', { credentials: 'omit' }); const body = await response.json(); if (!response.ok || body.code !== 0) throw new Error(body.msg || '服务暂不可用'); site.value = body.data; document.title = site.value!.config.seoTitle; const meta = document.querySelector('meta[name="description"]') || document.head.appendChild(document.createElement('meta')); meta.setAttribute('name', 'description'); meta.setAttribute('content', site.value!.config.seoDescription || ''); if (site.value!.brand.favicon) { const icon = document.querySelector<HTMLLinkElement>('link[rel="icon"]') || document.head.appendChild(document.createElement('link')); icon.rel = 'icon'; icon.href = site.value!.brand.favicon; } } catch (e) { error.value = e instanceof Error ? e.message : '请稍后重试'; } finally { loading.value = false } }
onMounted(load)
</script>
