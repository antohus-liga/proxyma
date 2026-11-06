<script setup lang="ts">
import { Inbox } from '@lucide/vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
  }>(),
  {
    description: undefined,
  },
)
</script>

<template>
  <div class="px-empty">
    <div class="px-empty__icon" aria-hidden="true">
      <slot name="icon">
        <Inbox :size="24" />
      </slot>
    </div>
    <p class="px-empty__title">{{ title }}</p>
    <p v-if="description" class="px-empty__description">{{ description }}</p>
    <div v-if="$slots.action" class="px-empty__action">
      <slot name="action" />
    </div>
  </div>
</template>

<style scoped>
.px-empty {
  display: flex;
  min-height: 240px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--px-space-2);
  padding: var(--px-space-8) var(--px-space-4);
  text-align: center;
}

.px-empty__icon {
  display: inline-flex;
  color: var(--px-color-text-tertiary);
}

.px-empty__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h4);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
}

.px-empty__description {
  max-width: 400px;
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
  text-wrap: pretty;
}

.px-empty__action {
  margin-block-start: var(--px-space-2);
}
</style>
