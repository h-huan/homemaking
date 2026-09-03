<template><div class="section-heading"><p class="eyebrow">{{ eyebrow }}</p><h2>{{ title }}</h2><p v-if="subtitle" class="section-subtitle">{{ subtitle }}</p></div></template>
<script setup lang="ts">defineProps<{title:string;subtitle?:string;eyebrow?:string}>()</script>
