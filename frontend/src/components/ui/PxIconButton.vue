<script setup lang="ts">
import { useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

type PxIconButtonVariant = 'ghost' | 'secondary' | 'danger'
type PxIconButtonSize = 'sm' | 'md' | 'lg'

withDefaults(
  defineProps<{
    label: string
    variant?: PxIconButtonVariant
    size?: PxIconButtonSize
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
  }>(),
  {
    variant: 'ghost',
    size: 'md',
    type: 'button',
    disabled: false,
  },
)

const attrs = useAttrs()
</script>

<template>
  <button
    v-bind="attrs"
    class="px-icon-button"
    :class="[`px-icon-button--${variant}`, `px-icon-button--${size}`]"
    :type="type"
    :aria-label="label"
    :title="label"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>

<style scoped>
.px-icon-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0;
  border: var(--px-border-width) solid transparent;
  border-radius: var(--px-radius-control);
  background: transparent;
  color: var(--px-color-text-secondary);
  cursor: pointer;
  transition:
    background-color var(--px-duration-fast) var(--px-ease-out),
    border-color var(--px-duration-fast) var(--px-ease-out),
    color var(--px-duration-fast) var(--px-ease-out);
}

.px-icon-button--sm {
  width: 28px;
  height: 28px;
}

.px-icon-button--md {
  width: 36px;
  height: 36px;
}

.px-icon-button--lg {
  width: 44px;
  height: 44px;
}

.px-icon-button--ghost:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
}

.px-icon-button--secondary {
  border-color: var(--px-color-border-strong);
  background: var(--px-color-surface);
  color: var(--px-color-text-primary);
}

.px-icon-button--secondary:hover {
  background: var(--px-color-surface-subtle);
}

.px-icon-button--danger {
  color: var(--px-color-danger-fg);
}

.px-icon-button--danger:hover {
  background: var(--px-color-danger-bg);
}

.px-icon-button:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.px-icon-button:disabled {
  opacity: var(--px-state-disabled);
  cursor: not-allowed;
}

@media (max-width: 767px) {
  .px-icon-button--md {
    width: 44px;
    height: 44px;
  }
}
</style>
