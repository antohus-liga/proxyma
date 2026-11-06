<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { RouterLink } from 'vue-router'

type PxButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'link'
type PxButtonSize = 'sm' | 'md' | 'lg'

defineOptions({ inheritAttrs: false })

const props = withDefaults(
  defineProps<{
    variant?: PxButtonVariant
    size?: PxButtonSize
    to?: string
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    loading?: boolean
    fullWidth?: boolean
  }>(),
  {
    variant: 'primary',
    size: 'md',
    type: 'button',
    disabled: false,
    loading: false,
    fullWidth: false,
  },
)

const attrs = useAttrs()

const isLink = computed(() => Boolean(props.to))
const isInactive = computed(() => props.disabled || props.loading)
const component = computed(() => (isLink.value ? RouterLink : 'button'))

const componentProps = computed(() =>
  isLink.value
    ? {
        to: props.to,
        'aria-disabled': isInactive.value ? 'true' : undefined,
      }
    : {
        type: props.type,
        disabled: isInactive.value,
        'aria-busy': props.loading ? 'true' : undefined,
      },
)
</script>

<template>
  <component
    :is="component"
    v-bind="{ ...componentProps, ...attrs }"
    class="px-button"
    :class="[
      `px-button--${variant}`,
      `px-button--${size}`,
      {
        'px-button--full': fullWidth,
        'px-button--loading': loading,
        'px-button--inactive': isInactive,
      },
    ]"
  >
    <span v-if="loading" class="px-button__spinner" aria-hidden="true" />
    <slot v-else name="icon" />
    <span class="px-button__label"><slot /></span>
  </component>
</template>

<style scoped>
.px-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--px-space-2);
  min-height: 36px;
  padding: 0 14px;
  border: var(--px-border-width) solid transparent;
  border-radius: var(--px-radius-control);
  font-family: var(--px-font-sans);
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-control);
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  transition:
    background-color var(--px-duration-fast) var(--px-ease-out),
    border-color var(--px-duration-fast) var(--px-ease-out),
    color var(--px-duration-fast) var(--px-ease-out);
}

.px-button--sm {
  min-height: 28px;
  padding-inline: 10px;
  font-size: var(--px-font-size-small);
}

.px-button--md {
  min-height: 36px;
  padding-inline: 14px;
}

.px-button--lg {
  min-height: 44px;
  padding-inline: 18px;
  font-size: var(--px-font-size-body-strong);
}

.px-button--primary {
  background: var(--px-color-surface-inverse);
  color: var(--px-color-text-inverse);
}

.px-button--primary:hover {
  background: var(--px-color-surface-inverse-hover);
  color: var(--px-color-text-inverse);
  text-decoration: none;
}

.px-button--primary:active {
  background: var(--px-color-surface-inverse-active);
}

.px-button--secondary {
  border-color: var(--px-color-border-strong);
  background: var(--px-color-surface);
  color: var(--px-color-text-primary);
}

.px-button--secondary:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.px-button--secondary:active {
  background: var(--px-state-active);
}

.px-button--ghost {
  background: transparent;
  color: var(--px-color-text-secondary);
}

.px-button--ghost:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.px-button--ghost:active {
  background: var(--px-state-active);
}

.px-button--danger {
  background: var(--px-color-danger-solid);
  color: var(--px-color-danger-contrast);
}

.px-button--danger:hover {
  background: var(--px-color-danger-fg);
  color: var(--px-color-danger-contrast);
  text-decoration: none;
}

.px-button--danger:active {
  background: var(--px-color-danger-fg);
}

.px-button--link {
  min-height: 0;
  padding: 0;
  border-color: transparent;
  background: transparent;
  color: var(--px-color-accent);
}

.px-button--link:hover {
  color: var(--px-color-accent-hover);
  text-decoration: underline;
}

.px-button--link:active {
  color: var(--px-color-accent-active);
}

.px-button--full {
  width: 100%;
}

.px-button:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.px-button--inactive {
  opacity: var(--px-state-disabled);
  cursor: not-allowed;
}

.px-button--inactive:hover {
  text-decoration: none;
}

.px-button__label {
  display: inline-flex;
  align-items: center;
}

.px-button__spinner {
  width: 14px;
  height: 14px;
  border: 2px solid currentcolor;
  border-top-color: transparent;
  border-radius: var(--px-radius-full);
  animation: px-button-spin 700ms linear infinite;
}

@keyframes px-button-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
