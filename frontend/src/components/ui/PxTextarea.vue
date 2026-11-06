<script setup lang="ts">
import { useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

withDefaults(
  defineProps<{
    id?: string
    invalid?: boolean
    rows?: number
    maxlength?: number
  }>(),
  {
    invalid: false,
    rows: 4,
  },
)

const model = defineModel<string>({ default: '' })
const attrs = useAttrs()
</script>

<template>
  <textarea
    v-bind="attrs"
    :id="id"
    v-model="model"
    class="px-textarea"
    :class="{ 'px-textarea--invalid': invalid }"
    :rows="rows"
    :maxlength="maxlength"
    :aria-invalid="invalid ? 'true' : undefined"
  />
</template>

<style scoped>
.px-textarea {
  display: block;
  width: 100%;
  min-height: 96px;
  resize: vertical;
  padding: var(--px-space-3);
  border: var(--px-border-width) solid var(--px-color-border-strong);
  border-radius: var(--px-radius-control);
  background: var(--px-color-surface);
  color: var(--px-color-text-primary);
  font-family: var(--px-font-sans);
  font-size: var(--px-font-size-input);
  line-height: var(--px-line-body);
  transition:
    border-color var(--px-duration-fast) var(--px-ease-out),
    box-shadow var(--px-duration-fast) var(--px-ease-out);
}

.px-textarea::placeholder {
  color: var(--px-color-text-tertiary);
}

.px-textarea:focus-visible {
  border-color: var(--px-color-accent);
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.px-textarea--invalid {
  border-color: var(--px-color-danger-fg);
}

.px-textarea:disabled {
  border-color: var(--px-color-border-disabled);
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-disabled);
  cursor: not-allowed;
  opacity: var(--px-state-disabled);
}

@media (max-width: 767px) {
  .px-textarea {
    font-size: var(--px-font-size-body);
  }
}
</style>
