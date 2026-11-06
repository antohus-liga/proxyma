<script setup lang="ts">
import { computed, useId } from 'vue'
import { useI18n } from 'vue-i18n'
import { ChevronDown, Languages } from '@lucide/vue'
import { useLocale } from '@/composables/useLocale'
import { isSupportedLocale, SUPPORTED_LOCALES } from '@/i18n/locale'

const props = withDefaults(
  defineProps<{
    labelHidden?: boolean
  }>(),
  {
    labelHidden: false,
  },
)

const { t } = useI18n()
const { locale, setLocale } = useLocale()
const selectId = useId()

const options = computed(() =>
  SUPPORTED_LOCALES.map((value) => ({
    value,
    label: value === 'pt-PT' ? t('locale.portuguese') : t('locale.english'),
  })),
)

function onChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value

  if (isSupportedLocale(value)) {
    setLocale(value)
  }
}
</script>

<template>
  <div class="locale-control">
    <label
      class="locale-control__label"
      :class="{ 'px-visually-hidden': props.labelHidden }"
      :for="selectId"
    >
      {{ t('locale.label') }}
    </label>

    <div class="locale-control__field">
      <Languages class="locale-control__icon" :size="15" aria-hidden="true" />
      <select :id="selectId" class="locale-control__select" :value="locale" @change="onChange">
        <option v-for="option in options" :key="option.value" :value="option.value">
          {{ option.label }}
        </option>
      </select>
      <ChevronDown class="locale-control__chevron" :size="14" aria-hidden="true" />
    </div>
  </div>
</template>

<style scoped>
.locale-control {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-3);
}

.locale-control__label {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.locale-control__field {
  position: relative;
  display: inline-flex;
  min-width: 0;
  max-width: 100%;
  align-items: center;
}

.locale-control__select {
  min-width: 0;
  max-width: 100%;
  min-height: 44px;
  appearance: none;
  overflow: hidden;
  padding: 0 26px 0 28px;
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-control);
  background: var(--px-color-surface);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
  text-overflow: ellipsis;
  cursor: pointer;
}

.locale-control__select:hover {
  border-color: var(--px-color-border-strong);
  color: var(--px-color-text-primary);
}

.locale-control__select:focus-visible {
  border-color: var(--px-color-accent);
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.locale-control__icon,
.locale-control__chevron {
  position: absolute;
  color: var(--px-color-text-tertiary);
  pointer-events: none;
}

.locale-control__icon {
  inset-inline-start: 8px;
}

.locale-control__chevron {
  inset-inline-end: 7px;
}
</style>
