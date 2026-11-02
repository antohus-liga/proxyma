<script setup lang="ts">
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const steps = [
  {
    title: 'home.howItWorks.steps.account.title',
    body: 'home.howItWorks.steps.account.body',
  },
  {
    title: 'home.howItWorks.steps.organisation.title',
    body: 'home.howItWorks.steps.organisation.body',
  },
  {
    title: 'home.howItWorks.steps.traffic.title',
    body: 'home.howItWorks.steps.traffic.body',
  },
] as const
</script>

<template>
  <section class="how-it-works" aria-labelledby="how-it-works-title">
    <div class="px-container how-it-works__grid">
      <div class="how-it-works__intro">
        <h2 id="how-it-works-title" class="how-it-works__title">
          {{ t('home.howItWorks.title') }}
        </h2>
        <p class="how-it-works__intro-body">{{ t('home.howItWorks.intro') }}</p>
      </div>

      <ol class="how-it-works__steps" role="list">
        <li v-for="(step, index) in steps" :key="step.title" class="how-it-works__step">
          <span class="how-it-works__number" aria-hidden="true">
            {{ String(index + 1).padStart(2, '0') }}
          </span>
          <div class="how-it-works__step-content">
            <h3 class="how-it-works__step-title">{{ t(step.title) }}</h3>
            <p class="how-it-works__step-body">{{ t(step.body) }}</p>
          </div>
        </li>
      </ol>
    </div>
  </section>
</template>

<style scoped>
.how-it-works {
  padding-block: var(--px-space-20);
}

.how-it-works__grid {
  display: grid;
  grid-template-columns: 5fr 7fr;
  align-items: start;
  gap: var(--px-space-16);
}

.how-it-works__intro {
  max-width: 320px;
}

.how-it-works__title {
  margin-bottom: var(--px-space-2);
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h2);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
  letter-spacing: var(--px-tracking-title);
}

.how-it-works__intro-body {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.how-it-works__steps {
  margin: 0;
  padding: 0;
  list-style: none;
}

.how-it-works__step {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: var(--px-space-3);
  padding-block: var(--px-space-5);
  border-top: var(--px-border-width) solid var(--px-color-border-subtle);
}

.how-it-works__step:first-child {
  padding-block-start: 0;
  border-top: 0;
}

.how-it-works__number {
  padding-block-start: 2px;
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-control);
  font-variant-numeric: tabular-nums;
}

.how-it-works__step-title {
  margin-bottom: 6px;
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h3);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-title);
  letter-spacing: var(--px-tracking-card);
}

.how-it-works__step-body {
  max-width: 52ch;
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

@media (max-width: 1023px) {
  .how-it-works__grid {
    grid-template-columns: 1fr;
    gap: var(--px-space-8);
  }

  .how-it-works__intro {
    max-width: 640px;
  }
}

@media (max-width: 767px) {
  .how-it-works {
    padding-block: var(--px-space-16);
  }
}

@media (max-width: 479px) {
  .how-it-works {
    padding-block: var(--px-space-12);
  }

  .how-it-works__step {
    grid-template-columns: 28px minmax(0, 1fr);
    gap: var(--px-space-2);
  }
}
</style>
