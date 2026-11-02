<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
</script>

<template>
  <a class="px-skip-link" href="#main-content">{{ t('a11y.skipToContent') }}</a>
  <RouterView />
</template>
