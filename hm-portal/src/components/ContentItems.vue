<template><div class="content-items"><article v-for="item in items" :key="item.title"><img v-if="item.image" :src="item.image" :alt="item.title" loading="lazy"/><h3>{{ item.title }}</h3><p>{{ item.text }}</p></article></div></template>
<script setup lang="ts">import type { ContentItem } from '../types';defineProps<{items?:ContentItem[]}>()</script>
