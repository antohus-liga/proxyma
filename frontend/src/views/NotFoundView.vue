<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'

const { t } = useI18n()
</script>

<template>
  <main id="main-content" class="not-found" tabindex="-1">
    <h1 class="not-found__title">{{ t('notFound.title') }}</h1>
    <p class="not-found__description">{{ t('notFound.description') }}</p>
    <RouterLink class="not-found__link" to="/">{{ t('notFound.back') }}</RouterLink>
  </main>
</template>

<style scoped>
.not-found {
  min-height: 100dvh;
  display: grid;
  align-content: center;
  justify-items: start;
  gap: var(--px-space-4);
  max-width: var(--px-prose-max);
  padding: var(--px-page-padding-sm);
  margin-inline: auto;
}

.not-found__title {
  font-size: var(--px-font-size-h1);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-heading);
  letter-spacing: var(--px-tracking-heading);
}

.not-found__description {
  color: var(--px-color-text-secondary);
}

.not-found__link {
  font-weight: var(--px-font-weight-medium);
}
</style>
