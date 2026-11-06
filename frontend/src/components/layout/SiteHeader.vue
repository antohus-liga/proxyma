<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'

const { t } = useI18n()
</script>

<template>
  <header class="site-header">
    <div class="px-container site-header__inner">
      <RouterLink class="site-header__wordmark" to="/" :aria-label="t('app.homeLabel')">
        {{ t('app.wordmark') }}
      </RouterLink>

      <nav class="site-header__nav" :aria-label="t('nav.account')">
        <RouterLink class="site-header__link" to="/sign-in">
          {{ t('nav.signIn') }}
        </RouterLink>
        <RouterLink class="site-header__link site-header__link--emphasis" to="/create-account">
          {{ t('nav.createAccount') }}
        </RouterLink>
      </nav>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  border-bottom: var(--px-border-width) solid var(--px-color-border-subtle);
}

.site-header__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-4);
  min-height: 72px;
  padding-block: var(--px-space-3);
}

.site-header__wordmark {
  color: var(--px-color-text-primary);
  font-size: 20px;
  font-weight: var(--px-font-weight-semibold);
  line-height: 1;
  letter-spacing: -0.02em;
}

.site-header__wordmark:hover {
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.site-header__nav {
  display: flex;
  align-items: center;
  gap: var(--px-space-1);
}

.site-header__link {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  padding-inline: 10px;
  border-radius: var(--px-radius-control);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-control);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-control);
}

.site-header__link:hover {
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.site-header__link--emphasis {
  color: var(--px-color-text-primary);
}

@media (max-width: 767px) {
  .site-header__inner {
    min-height: 64px;
    padding-block: var(--px-space-2);
  }
}

@media (max-width: 479px) {
  .site-header__wordmark {
    font-size: 18px;
  }

  .site-header__nav {
    gap: 0;
  }

  .site-header__link {
    padding-inline: var(--px-space-2);
    font-size: var(--px-font-size-small);
  }
}
</style>
