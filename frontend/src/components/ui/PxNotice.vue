<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, Info, TriangleAlert } from '@lucide/vue'

type NoticeTone = 'danger' | 'info' | 'success' | 'warning'

const props = withDefaults(
  defineProps<{
    tone?: NoticeTone
    role?: 'alert' | 'status'
    title?: string
  }>(),
  {
    tone: 'info',
  },
)

const icon = computed(() => {
  if (props.tone === 'success') {
    return CircleCheck
  }

  if (props.tone === 'warning') {
    return TriangleAlert
  }

  if (props.tone === 'danger') {
    return TriangleAlert
  }

  return Info
})

const resolvedRole = computed(() => {
  if (props.role) {
    return props.role
  }

  return props.tone === 'danger' ? 'alert' : 'status'
})
</script>

<template>
  <div class="px-notice" :class="`px-notice--${tone}`" :role="resolvedRole">
    <component :is="icon" class="px-notice__icon" :size="16" aria-hidden="true" />
    <div class="px-notice__content">
      <p v-if="title" class="px-notice__title">{{ title }}</p>
      <div class="px-notice__body">
        <slot />
      </div>
    </div>
  </div>
</template>

<style scoped>
.px-notice {
  display: flex;
  align-items: flex-start;
  gap: var(--px-space-2);
  padding: var(--px-space-3);
  border: var(--px-border-width) solid;
  border-radius: var(--px-radius-control);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
}

.px-notice__icon {
  flex-shrink: 0;
  margin-block-start: 1px;
}

.px-notice__content {
  min-width: 0;
}

.px-notice__title {
  margin-block-end: var(--px-space-1);
  font-weight: var(--px-font-weight-semibold);
}

.px-notice__body {
  text-wrap: pretty;
}

.px-notice--danger {
  border-color: var(--px-color-danger-border);
  background: var(--px-color-danger-bg);
  color: var(--px-color-danger-fg);
}

.px-notice--info {
  border-color: var(--px-color-info-border);
  background: var(--px-color-info-bg);
  color: var(--px-color-info-fg);
}

.px-notice--success {
  border-color: var(--px-color-success-border);
  background: var(--px-color-success-bg);
  color: var(--px-color-success-fg);
}

.px-notice--warning {
  border-color: var(--px-color-warning-border);
  background: var(--px-color-warning-bg);
  color: var(--px-color-warning-fg);
}
</style>
