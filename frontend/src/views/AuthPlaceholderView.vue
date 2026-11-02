<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'

const route = useRoute()
const { t } = useI18n()

const mode = computed(() => (route.meta.authMode === 'createAccount' ? 'createAccount' : 'signIn'))
const title = computed(() => t(`auth.placeholder.${mode.value}.title`))
const body = computed(() => t(`auth.placeholder.${mode.value}.body`))
</script>

<template>
  <main id="main-content" class="auth-placeholder" tabindex="-1">
    <div class="auth-placeholder__inner">
      <RouterLink class="auth-placeholder__wordmark" to="/" :aria-label="t('app.homeLabel')">
        {{ t('app.wordmark') }}
      </RouterLink>

      <h1 class="auth-placeholder__title">{{ title }}</h1>
      <p class="auth-placeholder__body">{{ body }}</p>
      <RouterLink class="auth-placeholder__back" to="/">
        {{ t('auth.placeholder.back') }}
      </RouterLink>
    </div>
  </main>
</template>

<style scoped>
.auth-placeholder {
  display: grid;
  place-items: center;
  min-height: 100dvh;
  padding: var(--px-page-padding-sm);
}

.auth-placeholder__inner {
  width: 100%;
  max-width: var(--px-auth-card-max);
}

.auth-placeholder__wordmark {
  display: inline-block;
  margin-bottom: var(--px-space-8);
  color: var(--px-color-text-primary);
  font-size: 20px;
  font-weight: var(--px-font-weight-semibold);
  line-height: 1;
  letter-spacing: -0.02em;
}

.auth-placeholder__wordmark:hover {
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.auth-placeholder__title {
  margin-bottom: var(--px-space-2);
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-h1);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-heading);
  letter-spacing: var(--px-tracking-heading);
}

.auth-placeholder__body {
  margin-bottom: var(--px-space-6);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
}

.auth-placeholder__back {
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-medium);
}

@media (max-width: 767px) {
  .auth-placeholder__wordmark {
    margin-bottom: var(--px-space-6);
  }

  .auth-placeholder__title {
    font-size: var(--px-font-size-h2);
  }
}
</style>
