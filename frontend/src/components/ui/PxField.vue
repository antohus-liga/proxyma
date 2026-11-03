<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  label: string
  help?: string
  error?: string
  required?: boolean
  inputId?: string
}>()

const { t } = useI18n()
const generatedId = useId()

const id = computed(() => props.inputId ?? `px-field-${generatedId}`)
const helpId = computed(() => `${id.value}-help`)
const errorId = computed(() => `${id.value}-error`)
const describedBy = computed(() => {
  const ids: string[] = []

  if (props.help) {
    ids.push(helpId.value)
  }

  if (props.error) {
    ids.push(errorId.value)
  }

  return ids.length > 0 ? ids.join(' ') : undefined
})
</script>

<template>
  <div class="px-field" :data-invalid="Boolean(error)">
    <label class="px-field__label" :for="id">
      {{ label }}
      <span v-if="required" class="px-field__required">({{ t('common.required') }})</span>
    </label>

    <slot :id="id" :described-by="describedBy" :invalid="Boolean(error)" />

    <p v-if="help" :id="helpId" class="px-field__help">{{ help }}</p>
    <p v-if="error" :id="errorId" class="px-field__error">
      <slot name="error">{{ error }}</slot>
    </p>
  </div>
</template>

<style scoped>
.px-field {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-1);
}

.px-field__label {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-small-strong);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.px-field__required {
  color: var(--px-color-text-tertiary);
  font-weight: var(--px-font-weight-regular);
}

.px-field__help,
.px-field__error {
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
}

.px-field__help {
  color: var(--px-color-text-tertiary);
}

.px-field__error {
  color: var(--px-color-danger-fg);
  font-weight: var(--px-font-weight-medium);
}

.px-field__error :slotted(a) {
  color: inherit;
  text-decoration: underline;
  text-underline-offset: 0.15em;
}

.px-field__error :slotted(a:hover) {
  color: var(--px-color-danger-fg);
}
</style>
