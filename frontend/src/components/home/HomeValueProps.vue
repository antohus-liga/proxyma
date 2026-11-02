<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const items = [
  { title: 'home.valueProps.proxy.title', body: 'home.valueProps.proxy.body' },
  { title: 'home.valueProps.traffic.title', body: 'home.valueProps.traffic.body' },
  { title: 'home.valueProps.changes.title', body: 'home.valueProps.changes.body' },
] as const
</script>

<template>
  <section class="value-props" aria-labelledby="value-props-title">
    <div class="px-container">
      <h2 id="value-props-title" class="value-props__title">
        {{ t('home.valueProps.title') }}
      </h2>

      <ul class="value-props__list" role="list">
        <li v-for="item in items" :key="item.title" class="value-props__item">
          <h3 class="value-props__item-title">{{ t(item.title) }}</h3>
          <p class="value-props__item-body">{{ t(item.body) }}</p>
        </li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.value-props {
  padding-block: var(--px-space-10);
  border-block: var(--px-border-width) solid var(--px-color-border-subtle);
}

.value-props__title {
  margin-bottom: var(--px-space-6);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.value-props__list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.value-props__item {
  min-width: 0;
  padding-inline: var(--px-space-8);
}

.value-props__item:first-child {
  padding-inline-start: 0;
}

.value-props__item:last-child {
  padding-inline-end: 0;
}

.value-props__item + .value-props__item {
  border-inline-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

.value-props__item-title {
  margin-bottom: var(--px-space-2);
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h3);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
  letter-spacing: var(--px-tracking-card);
}

.value-props__item-body {
  max-width: 34ch;
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

@media (max-width: 1023px) {
  .value-props__list {
    grid-template-columns: 1fr;
  }

  .value-props__item {
    padding-inline: 0;
    padding-block: var(--px-space-5);
  }

  .value-props__item:first-child {
    padding-block-start: 0;
  }

  .value-props__item:last-child {
    padding-block-end: 0;
  }

  .value-props__item + .value-props__item {
    border-inline-start: 0;
    border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
  }

  .value-props__item-body {
    max-width: 60ch;
  }
}

@media (max-width: 767px) {
  .value-props {
    padding-block: var(--px-space-8);
  }

  .value-props__title {
    margin-bottom: var(--px-space-5);
  }
}
</style>
