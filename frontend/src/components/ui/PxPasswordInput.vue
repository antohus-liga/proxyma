<script setup lang="ts">
import { computed, ref, useAttrs } from 'vue'
import { useI18n } from 'vue-i18n'
import PxInput from './PxInput.vue'

defineOptions({ inheritAttrs: false })

const props = defineProps<{
  id: string
  invalid?: boolean
  autocomplete?: string
  required?: boolean
  minlength?: number
  maxlength?: number
  describedBy?: string
}>()

const model = defineModel<string>({ default: '' })
const { t } = useI18n()
const attrs = useAttrs()
const revealed = ref(false)

const toggleLabel = computed(() =>
  revealed.value ? t('auth.password.hideLabel') : t('auth.password.showLabel'),
)
</script>

<template>
  <div class="px-password">
    <PxInput
      v-bind="attrs"
      :id="props.id"
      v-model="model"
      class="px-password__input"
      :type="revealed ? 'text' : 'password'"
      :invalid="invalid"
      :autocomplete="autocomplete"
      :required="required"
      :minlength="minlength"
      :maxlength="maxlength"
      :aria-describedby="describedBy"
    />
    <button
      type="button"
      class="px-password__toggle"
      :aria-label="toggleLabel"
      :aria-controls="props.id"
      @click="revealed = !revealed"
    >
      {{ revealed ? t('auth.password.hide') : t('auth.password.show') }}
    </button>
  </div>
</template>

<style scoped>
.px-password {
  position: relative;
  display: flex;
}

.px-password :deep(.px-password__input) {
  padding-inline-end: 68px;
}

.px-password__toggle {
  position: absolute;
  inset-block: 0;
  inset-inline-end: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding-inline: var(--px-space-3);
  border: 0;
  border-radius: 0 var(--px-radius-control) var(--px-radius-control) 0;
  background: transparent;
  color: var(--px-color-accent);
  font-family: var(--px-font-sans);
  font-size: var(--px-font-size-caption);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
  cursor: pointer;
}

.px-password__toggle:hover {
  color: var(--px-color-accent-hover);
}

.px-password__toggle:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

@media (max-width: 767px) {
  .px-password__toggle {
    min-width: 64px;
  }
}
</style>
