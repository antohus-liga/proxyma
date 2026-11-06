<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'

const { t } = useI18n()
const route = useRoute()

const sections = [
  { labelKey: 'settings.sections.account', to: '/settings/account' },
  { labelKey: 'settings.sections.general', to: '/settings/general' },
  { labelKey: 'settings.sections.security', to: '/settings/security' },
]

function isActive(to: string): boolean {
  return route.path === to || route.path.startsWith(`${to}/`)
}
</script>

<template>
  <nav class="settings-section-nav" :aria-label="t('settings.sections.label')">
    <ul class="settings-section-nav__list">
      <li v-for="section in sections" :key="section.to">
        <RouterLink
          class="settings-section-nav__link"
          :class="{ 'settings-section-nav__link--active': isActive(section.to) }"
          :to="section.to"
          :aria-current="isActive(section.to) ? 'page' : undefined"
        >
          {{ t(section.labelKey) }}
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.settings-section-nav__list {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.settings-section-nav__link {
  position: relative;
  display: block;
  padding: var(--px-space-2) var(--px-space-3);
  border-radius: var(--px-radius-control);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-control);
  transition:
    background-color var(--px-duration-fast) var(--px-ease-out),
    color var(--px-duration-fast) var(--px-ease-out);
}

.settings-section-nav__link:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.settings-section-nav__link--active {
  background: var(--px-color-accent-subtle);
  color: var(--px-color-accent);
}

.settings-section-nav__link--active::before {
  position: absolute;
  inset-block: 8px;
  inset-inline-start: 0;
  width: 2px;
  border-radius: var(--px-radius-full);
  background: var(--px-color-accent);
  content: '';
}

.settings-section-nav__link--active:hover {
  background: var(--px-color-accent-subtle);
  color: var(--px-color-accent);
}

.settings-section-nav__link:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

@media (max-width: 1023px) {
  .settings-section-nav__list {
    flex-direction: row;
    gap: var(--px-space-1);
    padding-block-end: var(--px-space-2);
    border-block-end: var(--px-border-width) solid var(--px-color-border-subtle);
    overflow-x: auto;
  }

  .settings-section-nav__link {
    white-space: nowrap;
  }

  .settings-section-nav__link--active::before {
    inset: auto auto 0 0;
    width: 100%;
    height: 2px;
  }
}
</style>
