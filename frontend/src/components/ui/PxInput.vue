<script setup lang="ts">
import { useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    id?: string
    type?: string
    invalid?: boolean
  }>(),
  {
    type: 'text',
    invalid: false,
  },
)

const model = defineModel<string>({ default: '' })
const attrs = useAttrs()
</script>

<template>
  <input
    v-bind="attrs"
    :id="id"
    v-model="model"
    class="px-input"
    :class="{ 'px-input--invalid': invalid }"
    :type="type"
    :aria-invalid="invalid ? 'true' : undefined"
  />
</template>

<style scoped>
.px-input {
  width: 100%;
  min-height: 36px;
  padding: 0 var(--px-space-3);
  border: var(--px-border-width) solid var(--px-color-border-strong);
  border-radius: var(--px-radius-control);
  background: var(--px-color-surface);
  color: var(--px-color-text-primary);
  font-family: var(--px-font-sans);
  font-size: var(--px-font-size-input);
  font-weight: var(--px-font-weight-regular);
  line-height: var(--px-line-control);
  transition:
    border-color var(--px-duration-fast) var(--px-ease-out),
    box-shadow var(--px-duration-fast) var(--px-ease-out);
}

.px-input::placeholder {
  color: var(--px-color-text-tertiary);
}

.px-input:hover {
  border-color: var(--px-color-border-strong);
}

.px-input:focus-visible {
  border-color: var(--px-color-accent);
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.px-input--invalid {
  border-color: var(--px-color-danger-fg);
}

.px-input:disabled {
  border-color: var(--px-color-border-disabled);
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-disabled);
  cursor: not-allowed;
  opacity: var(--px-state-disabled);
}

.px-input:read-only {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
}

@media (max-width: 767px) {
  .px-input {
    min-height: 44px;
    font-size: var(--px-font-size-body);
  }
}
</style>
