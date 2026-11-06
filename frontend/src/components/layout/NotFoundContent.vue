<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'

withDefaults(
  defineProps<{
    backTo?: string
    backLabel?: string
  }>(),
  {
    backTo: '/',
    backLabel: undefined,
  },
)

const { t } = useI18n()
</script>

<template>
  <section class="not-found-content">
    <h1 class="not-found-content__title">{{ t('notFound.title') }}</h1>
    <p class="not-found-content__description">{{ t('notFound.description') }}</p>
    <RouterLink class="not-found-content__link" :to="backTo">
      {{ backLabel ?? t('notFound.back') }}
    </RouterLink>
  </section>
</template>

<style scoped>
.not-found-content {
  display: flex;
  max-width: var(--px-prose-max);
  flex-direction: column;
  align-items: flex-start;
  gap: var(--px-space-3);
  padding-block: var(--px-space-10);
}

.not-found-content__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h1);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-heading);
  letter-spacing: var(--px-tracking-heading);
}

.not-found-content__title:focus {
  outline: none;
  box-shadow: none;
}

.not-found-content__description {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
}

.not-found-content__link {
  font-weight: var(--px-font-weight-medium);
}
</style>
