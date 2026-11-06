<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import LocaleControl from './LocaleControl.vue'

const { t } = useI18n()
const year = new Date().getFullYear()
</script>

<template>
  <footer class="site-footer">
    <div class="px-container site-footer__inner">
      <RouterLink class="site-footer__wordmark" to="/" :aria-label="t('app.homeLabel')">
        {{ t('app.wordmark') }}
      </RouterLink>

      <div class="site-footer__meta">
        <LocaleControl label-hidden />
        <RouterLink class="site-footer__link" to="/sign-in">{{ t('nav.signIn') }}</RouterLink>
        <p class="site-footer__copyright">{{ t('footer.copyright', { year }) }}</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.site-footer {
  border-top: var(--px-border-width) solid var(--px-color-border-subtle);
}

.site-footer__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-4);
  padding-block: var(--px-space-8);
}

.site-footer__wordmark {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-body-strong);
  font-weight: var(--px-font-weight-semibold);
  line-height: 1;
  letter-spacing: -0.02em;
}

.site-footer__wordmark:hover {
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.site-footer__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: var(--px-space-4) var(--px-space-6);
}

.site-footer__link {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.site-footer__link:hover {
  color: var(--px-color-text-primary);
}

.site-footer__copyright {
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 767px) {
  .site-footer__inner {
    flex-wrap: wrap;
    padding-block: var(--px-space-6);
  }

  .site-footer__meta {
    gap: var(--px-space-4);
  }
}

@media (max-width: 479px) {
  .site-footer__inner {
    padding-block: var(--px-space-5);
  }

  .site-footer__meta {
    gap: var(--px-space-3);
  }
}
</style>
