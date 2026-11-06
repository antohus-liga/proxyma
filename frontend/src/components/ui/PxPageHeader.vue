<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    eyebrow?: string
  }>(),
  {
    description: undefined,
    eyebrow: undefined,
  },
)
</script>

<template>
  <header class="px-page-header">
    <div class="px-page-header__main">
      <p v-if="eyebrow" class="px-page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="px-page-header__title">{{ title }}</h1>
      <p v-if="description" class="px-page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="px-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<style scoped>
.px-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--px-space-6);
  margin-block-end: var(--px-space-6);
}

.px-page-header__main {
  min-width: 0;
}

.px-page-header__eyebrow {
  margin-block-end: var(--px-space-2);
  color: var(--px-color-accent);
  font-size: var(--px-font-size-caption);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-compact);
  letter-spacing: var(--px-tracking-label);
  text-transform: uppercase;
}

.px-page-header__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h1);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-heading);
  letter-spacing: var(--px-tracking-heading);
  text-wrap: balance;
}

.px-page-header__title:focus {
  outline: none;
  box-shadow: none;
}

.px-page-header__description {
  max-width: 720px;
  margin-block-start: var(--px-space-2);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.px-page-header__actions {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--px-space-2);
}

@media (max-width: 767px) {
  .px-page-header {
    flex-direction: column;
    gap: var(--px-space-4);
  }

  .px-page-header__title {
    font-size: var(--px-font-size-h2);
  }

  .px-page-header__actions {
    width: 100%;
  }
}
</style>
