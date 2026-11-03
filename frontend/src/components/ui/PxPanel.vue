<script setup lang="ts">
import { useId } from 'vue'

withDefaults(
  defineProps<{
    title?: string
    description?: string
    flush?: boolean
  }>(),
  {
    title: undefined,
    description: undefined,
    flush: false,
  },
)

const titleId = useId()
</script>

<template>
  <section class="px-panel" :aria-labelledby="title ? titleId : undefined">
    <header v-if="title || description || $slots.actions" class="px-panel__header">
      <div class="px-panel__heading">
        <h2 v-if="title" :id="titleId" class="px-panel__title">{{ title }}</h2>
        <p v-if="description" class="px-panel__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="px-panel__actions">
        <slot name="actions" />
      </div>
    </header>

    <div class="px-panel__body" :class="{ 'px-panel__body--flush': flush }">
      <slot />
    </div>

    <footer v-if="$slots.footer" class="px-panel__footer">
      <slot name="footer" />
    </footer>
  </section>
</template>

<style scoped>
.px-panel {
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-card);
  background: var(--px-color-surface);
}

.px-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--px-space-4);
  padding: var(--px-space-6) var(--px-space-6) 0;
}

.px-panel__heading {
  min-width: 0;
}

.px-panel__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h4);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
  letter-spacing: var(--px-tracking-card);
}

.px-panel__description {
  margin-block-start: var(--px-space-1);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
  text-wrap: pretty;
}

.px-panel__actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: var(--px-space-2);
}

.px-panel__body {
  padding: var(--px-space-5) var(--px-space-6) var(--px-space-6);
}

.px-panel__body--flush {
  padding: 0;
}

.px-panel__footer {
  padding: var(--px-space-4) var(--px-space-6);
  border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

@media (max-width: 767px) {
  .px-panel__header {
    flex-direction: column;
    padding: var(--px-space-5) var(--px-space-5) 0;
  }

  .px-panel__actions {
    width: 100%;
  }

  .px-panel__body {
    padding: var(--px-space-4) var(--px-space-5) var(--px-space-5);
  }

  .px-panel__body--flush {
    padding: 0;
  }

  .px-panel__footer {
    padding: var(--px-space-4) var(--px-space-5);
  }
}
</style>
