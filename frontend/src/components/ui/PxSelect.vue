<script setup lang="ts">
import { useAttrs } from 'vue'
import { ChevronDown } from '@lucide/vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    id?: string
    invalid?: boolean
    disabled?: boolean
  }>(),
  {
    invalid: false,
    disabled: false,
  },
)

const model = defineModel<string>({ default: '' })
const attrs = useAttrs()
</script>

<template>
  <div class="px-select" :class="{ 'px-select--disabled': disabled }">
    <select
      v-bind="attrs"
      :id="id"
      v-model="model"
      class="px-select__control"
      :class="{ 'px-select__control--invalid': invalid }"
      :disabled="disabled"
      :aria-invalid="invalid ? 'true' : undefined"
    >
      <slot />
    </select>
    <ChevronDown class="px-select__icon" :size="16" aria-hidden="true" />
  </div>
</template>

<style scoped>
.px-select {
  position: relative;
  display: flex;
  width: 100%;
}

.px-select__control {
  width: 100%;
  min-height: 36px;
  appearance: none;
  padding: 0 36px 0 var(--px-space-3);
  border: var(--px-border-width) solid var(--px-color-border-strong);
  border-radius: var(--px-radius-control);
  background: var(--px-color-surface);
  color: var(--px-color-text-primary);
  font-family: var(--px-font-sans);
  font-size: var(--px-font-size-input);
  line-height: var(--px-line-control);
  cursor: pointer;
  transition:
    border-color var(--px-duration-fast) var(--px-ease-out),
    box-shadow var(--px-duration-fast) var(--px-ease-out);
}

.px-select__control:focus-visible {
  border-color: var(--px-color-accent);
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.px-select__control--invalid {
  border-color: var(--px-color-danger-fg);
}

.px-select__icon {
  position: absolute;
  inset-block-start: 50%;
  inset-inline-end: var(--px-space-3);
  color: var(--px-color-text-tertiary);
  pointer-events: none;
  transform: translateY(-50%);
}

.px-select--disabled .px-select__control {
  border-color: var(--px-color-border-disabled);
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-disabled);
  cursor: not-allowed;
  opacity: var(--px-state-disabled);
}

@media (max-width: 767px) {
  .px-select__control {
    min-height: 44px;
    font-size: var(--px-font-size-body);
  }
}
</style>
