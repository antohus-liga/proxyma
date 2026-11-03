<script setup lang="ts">
import { useId } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'

defineProps<{
  title: string
  description?: string
}>()

const { t } = useI18n()
const titleId = useId()
</script>

<template>
  <main id="main-content" class="auth-page">
    <section class="auth-card" :aria-labelledby="titleId">
      <RouterLink class="auth-card__wordmark" to="/" :aria-label="t('app.homeLabel')">
        {{ t('app.wordmark') }}
      </RouterLink>

      <h1 :id="titleId" class="auth-card__title" tabindex="-1">{{ title }}</h1>
      <p v-if="description" class="auth-card__description">{{ description }}</p>

      <div class="auth-card__body">
        <slot />
      </div>

      <div v-if="$slots.footer" class="auth-card__footer">
        <slot name="footer" />
      </div>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  display: grid;
  min-height: 100dvh;
  place-items: center;
  padding: var(--px-space-12) var(--px-page-padding-sm);
}

.auth-card {
  width: 100%;
  max-width: var(--px-auth-card-max);
  padding: var(--px-space-8);
  border: var(--px-border-width) solid var(--px-color-border-subtle);
  border-radius: var(--px-radius-card);
  background: var(--px-color-surface);
}

.auth-card__wordmark {
  display: inline-block;
  margin-bottom: var(--px-space-8);
  color: var(--px-color-text-primary);
  font-size: 20px;
  font-weight: var(--px-font-weight-semibold);
  line-height: 1;
  letter-spacing: -0.02em;
}

.auth-card__wordmark:hover {
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.auth-card__title {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h1);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-heading);
  letter-spacing: var(--px-tracking-heading);
  text-wrap: balance;
}

.auth-card__title:focus {
  outline: none;
  box-shadow: none;
}

.auth-card__description {
  margin-top: var(--px-space-2);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.auth-card__body {
  margin-top: var(--px-space-6);
}

.auth-card__footer {
  margin-top: var(--px-space-6);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
  text-align: center;
}

@media (max-width: 479px) {
  .auth-page {
    align-items: start;
    padding: var(--px-space-10) var(--px-page-padding-sm) var(--px-space-8);
  }

  .auth-card {
    padding: 0;
    border: 0;
    border-radius: 0;
    background: transparent;
  }

  .auth-card__wordmark {
    margin-bottom: var(--px-space-6);
  }

  .auth-card__title {
    font-size: var(--px-font-size-h2);
  }

  .auth-card__body,
  .auth-card__footer {
    margin-top: var(--px-space-5);
  }
}
</style>
