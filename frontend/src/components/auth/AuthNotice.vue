<script setup lang="ts">
import { computed } from 'vue'
import { CircleCheck, Info, TriangleAlert } from '@lucide/vue'

type NoticeTone = 'danger' | 'info' | 'success'

const props = withDefaults(
  defineProps<{
    tone?: NoticeTone
    role?: 'alert' | 'status'
  }>(),
  {
    tone: 'danger',
  },
)

const icon = computed(() => {
  if (props.tone === 'success') {
    return CircleCheck
  }

  if (props.tone === 'info') {
    return Info
  }

  return TriangleAlert
})

const resolvedRole = computed(() => props.role ?? (props.tone === 'danger' ? 'alert' : 'status'))
</script>

<template>
  <div class="auth-notice" :class="`auth-notice--${tone}`" :role="resolvedRole">
    <component :is="icon" class="auth-notice__icon" :size="16" aria-hidden="true" />
    <div class="auth-notice__content">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.auth-notice {
  display: flex;
  align-items: flex-start;
  gap: var(--px-space-2);
  padding: var(--px-space-3);
  border: var(--px-border-width) solid;
  border-radius: var(--px-radius-control);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
}

.auth-notice__icon {
  flex-shrink: 0;
  margin-block-start: 1px;
}

.auth-notice__content {
  min-width: 0;
}

.auth-notice--danger {
  border-color: var(--px-color-danger-border);
  background: var(--px-color-danger-bg);
  color: var(--px-color-danger-fg);
}

.auth-notice--info {
  border-color: var(--px-color-info-border);
  background: var(--px-color-info-bg);
  color: var(--px-color-info-fg);
}

.auth-notice--success {
  border-color: var(--px-color-success-border);
  background: var(--px-color-success-bg);
  color: var(--px-color-success-fg);
}
</style>
