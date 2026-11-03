<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import type { AppNavItem } from './nav'

defineProps<{
  items: AppNavItem[]
  label: string
}>()

const { t } = useI18n()
const route = useRoute()

defineEmits<{
  navigate: []
}>()

function isActive(item: AppNavItem): boolean {
  if (item.to === '/admin' || item.to === '/app') {
    return route.path === item.to
  }

  return route.path === item.to || route.path.startsWith(`${item.to}/`)
}
</script>

<template>
  <nav class="app-nav" :aria-label="label">
    <ul class="app-nav__list">
      <li v-for="item in items" :key="item.to" class="app-nav__item">
        <RouterLink
          class="app-nav__link"
          :class="{ 'app-nav__link--active': isActive(item) }"
          :to="item.to"
          :aria-current="isActive(item) ? 'page' : undefined"
          @click="$emit('navigate')"
        >
          <component :is="item.icon" class="app-nav__icon" :size="16" aria-hidden="true" />
          <span class="app-nav__label">{{ t(item.labelKey) }}</span>
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>

<style scoped>
.app-nav__list {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-1);
  margin: 0;
  padding: 0;
  list-style: none;
}

.app-nav__link {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 36px;
  padding: 0 var(--px-space-3);
  border-radius: var(--px-radius-control);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-control);
}

.app-nav__link:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.app-nav__link:focus-visible {
  outline: none;
  box-shadow: var(--px-focus-ring);
}

.app-nav__link--active {
  background: var(--px-color-accent-subtle);
  color: var(--px-color-accent);
}

.app-nav__link--active::before {
  position: absolute;
  inset-block: 8px;
  inset-inline-start: 0;
  width: 2px;
  border-radius: var(--px-radius-full);
  background: var(--px-color-accent);
  content: '';
}

.app-nav__link--active:hover {
  background: var(--px-color-accent-subtle);
  color: var(--px-color-accent);
}

.app-nav__icon {
  flex-shrink: 0;
  color: currentcolor;
}

.app-nav__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
