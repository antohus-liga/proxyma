<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  items: { label: string; to?: string }[]
  label: string
}>()
</script>

<template>
  <nav class="px-breadcrumbs" :aria-label="label">
    <ol class="px-breadcrumbs__list">
      <li v-for="(item, index) in items" :key="`${item.label}-${index}`" class="px-breadcrumbs__item">
        <RouterLink v-if="item.to" class="px-breadcrumbs__link" :to="item.to">
          {{ item.label }}
        </RouterLink>
        <span v-else class="px-breadcrumbs__current" aria-current="page">{{ item.label }}</span>
        <span
          v-if="index < items.length - 1"
          class="px-breadcrumbs__separator"
          aria-hidden="true"
        >
          /
        </span>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.px-breadcrumbs__list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--px-space-2);
  margin: 0;
  padding: 0;
  list-style: none;
}

.px-breadcrumbs__item {
  display: inline-flex;
  align-items: center;
  gap: var(--px-space-2);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
}

.px-breadcrumbs__link {
  color: var(--px-color-text-secondary);
}

.px-breadcrumbs__link:hover {
  color: var(--px-color-text-primary);
}

.px-breadcrumbs__current {
  color: var(--px-color-text-primary);
}

.px-breadcrumbs__separator {
  color: var(--px-color-text-tertiary);
}
</style>
