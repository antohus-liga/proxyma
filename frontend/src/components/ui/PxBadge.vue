<script setup lang="ts">
import type { BadgeTone } from './types'

withDefaults(
  defineProps<{
    tone?: BadgeTone
    dot?: boolean
  }>(),
  {
    tone: 'neutral',
    dot: false,
  },
)
</script>

<template>
  <span class="px-badge" :class="`px-badge--${tone}`">
    <span v-if="dot" class="px-badge__dot" aria-hidden="true" />
    <slot />
  </span>
</template>

<style scoped>
.px-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--px-space-2);
  height: 22px;
  padding: 0 6px;
  border: var(--px-border-width) solid transparent;
  border-radius: var(--px-radius-badge);
  font-size: var(--px-font-size-caption);
  font-weight: var(--px-font-weight-medium);
  line-height: 1;
  white-space: nowrap;
}

.px-badge__dot {
  width: 6px;
  height: 6px;
  flex-shrink: 0;
  border-radius: var(--px-radius-full);
  background: currentcolor;
}

.px-badge--neutral {
  border-color: var(--px-color-border-subtle);
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-secondary);
}

.px-badge--accent {
  border-color: var(--px-color-info-border);
  background: var(--px-color-accent-subtle);
  color: var(--px-color-accent);
}

.px-badge--success {
  border-color: var(--px-color-success-border);
  background: var(--px-color-success-bg);
  color: var(--px-color-success-fg);
}

.px-badge--warning {
  border-color: var(--px-color-warning-border);
  background: var(--px-color-warning-bg);
  color: var(--px-color-warning-fg);
}

.px-badge--danger {
  border-color: var(--px-color-danger-border);
  background: var(--px-color-danger-bg);
  color: var(--px-color-danger-fg);
}

.px-badge--info {
  border-color: var(--px-color-info-border);
  background: var(--px-color-info-bg);
  color: var(--px-color-info-fg);
}
</style>
