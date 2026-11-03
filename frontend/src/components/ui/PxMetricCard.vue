<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string
    hint?: string
    tone?: 'default' | 'warning' | 'danger'
  }>(),
  {
    hint: undefined,
    tone: 'default',
  },
)
</script>

<template>
  <div class="px-metric" :class="`px-metric--${tone}`">
    <p class="px-metric__label">{{ label }}</p>
    <p class="px-metric__value">{{ value }}</p>
    <p v-if="hint" class="px-metric__hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.px-metric {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-2);
  padding: var(--px-space-5);
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-card);
  background: var(--px-color-surface);
}

.px-metric__label {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.px-metric__value {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-metric);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-heading);
  letter-spacing: var(--px-tracking-heading);
  font-variant-numeric: tabular-nums;
}

.px-metric__hint {
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
}

.px-metric--warning .px-metric__value {
  color: var(--px-color-warning-fg);
}

.px-metric--danger .px-metric__value {
  color: var(--px-color-danger-fg);
}
</style>
