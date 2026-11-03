<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Monitor, Moon, Sun } from '@lucide/vue'
import { useTheme, type ThemePreference } from '@/composables/useTheme'

const { t } = useI18n()
const { preference, setPreference } = useTheme()

const options = computed(() => [
  { value: 'system' as const, label: t('theme.system'), icon: Monitor },
  { value: 'light' as const, label: t('theme.light'), icon: Sun },
  { value: 'dark' as const, label: t('theme.dark'), icon: Moon },
])

function select(value: ThemePreference) {
  setPreference(value)
}
</script>

<template>
  <div class="theme-control">
    <p class="theme-control__label">{{ t('theme.label') }}</p>
    <div class="theme-control__options" role="group" :aria-label="t('theme.label')">
      <button
        v-for="option in options"
        :key="option.value"
        type="button"
        class="theme-control__button"
        :class="{ 'theme-control__button--active': preference === option.value }"
        :aria-label="option.label"
        :aria-pressed="preference === option.value"
        :title="option.label"
        @click="select(option.value)"
      >
        <component :is="option.icon" :size="15" aria-hidden="true" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.theme-control {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-3);
}

.theme-control__label {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.theme-control__options {
  display: inline-flex;
  gap: var(--px-space-1);
  padding: 2px;
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-control);
  background: var(--px-color-surface);
}

.theme-control__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 26px;
  padding: 0;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--px-color-text-tertiary);
  cursor: pointer;
}

.theme-control__button:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
}

.theme-control__button--active {
  background: var(--px-color-accent-subtle);
  color: var(--px-color-accent);
}

.theme-control__button:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}
</style>
