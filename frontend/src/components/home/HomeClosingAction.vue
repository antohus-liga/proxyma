<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PxButton from '@/components/ui/PxButton.vue'

const { t } = useI18n()
</script>

<template>
  <section class="closing" aria-labelledby="closing-title">
    <div class="px-container closing__inner">
      <div class="closing__copy">
        <h2 id="closing-title" class="closing__title">{{ t('home.closing.title') }}</h2>
        <p class="closing__body">{{ t('home.closing.body') }}</p>
      </div>

      <PxButton class="closing__action" to="/create-account" size="lg">
        {{ t('nav.createAccount') }}
      </PxButton>
    </div>
  </section>
</template>

<style scoped>
.closing {
  padding-block: var(--px-space-20);
  border-top: var(--px-border-width) solid var(--px-color-border-subtle);
}

.closing__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-8);
}

.closing__copy {
  max-width: 640px;
}

.closing__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h2);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
  letter-spacing: var(--px-tracking-title);
  text-wrap: balance;
}

.closing__body {
  margin-top: var(--px-space-2);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.closing__action {
  flex-shrink: 0;
}

@media (max-width: 1023px) {
  .closing {
    padding-block: var(--px-space-16);
  }

  .closing__inner {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--px-space-6);
  }
}

@media (max-width: 767px) {
  .closing__inner {
    align-items: stretch;
  }

  .closing__action {
    width: 100%;
  }
}

@media (max-width: 479px) {
  .closing {
    padding-block: var(--px-space-12);
  }
}
</style>
