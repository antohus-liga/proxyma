<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PxButton from '@/components/ui/PxButton.vue'
import globePng from '@/assets/globe-proxyma.png'
import globeWebp from '@/assets/globe-proxyma.webp'
import globeWebp512 from '@/assets/globe-proxyma-512.webp'

const { t } = useI18n()
</script>

<template>
  <section class="hero">
    <div class="px-container hero__grid">
      <div class="hero__copy">
        <p class="hero__eyebrow">{{ t('home.hero.eyebrow') }}</p>
        <h1 class="hero__title">{{ t('home.hero.title') }}</h1>
        <p class="hero__lead">{{ t('home.hero.lead') }}</p>
        <p class="hero__body">{{ t('home.hero.body') }}</p>

        <div class="hero__actions">
          <PxButton class="hero__action" to="/create-account" size="lg">
            {{ t('nav.createAccount') }}
          </PxButton>
          <PxButton class="hero__action" to="/sign-in" size="lg" variant="secondary">
            {{ t('nav.signIn') }}
          </PxButton>
        </div>
      </div>

      <div class="hero__art">
        <picture class="hero__picture">
          <source
            :srcset="`${globeWebp512} 512w, ${globeWebp} 1024w`"
            sizes="(min-width: 1024px) 480px, 320px"
            type="image/webp"
          />
          <img
            class="hero__image"
            :src="globePng"
            alt=""
            width="1024"
            height="1024"
            decoding="async"
          />
        </picture>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero__grid {
  display: grid;
  grid-template-columns: 48fr 52fr;
  align-items: center;
  gap: var(--px-space-16);
  padding-block: var(--px-space-24) var(--px-space-20);
}

.hero__copy {
  min-width: 0;
  max-width: 520px;
}

.hero__eyebrow {
  margin-bottom: var(--px-space-4);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.hero__title {
  margin-bottom: var(--px-space-4);
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-display);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-display);
  letter-spacing: var(--px-tracking-display);
  text-wrap: balance;
}

.hero__title:focus {
  outline: none;
  box-shadow: none;
}

.hero__lead {
  margin-bottom: var(--px-space-3);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body-lg);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.hero__body {
  max-width: 52ch;
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-body);
  line-height: var(--px-line-body);
  text-wrap: pretty;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--px-space-3);
  margin-top: var(--px-space-8);
}

.hero__art {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 0;
}

.hero__picture {
  display: block;
  width: 100%;
  max-width: 480px;
}

.hero__image {
  display: block;
  width: 100%;
  height: auto;
}

@media (max-width: 1023px) {
  .hero__grid {
    grid-template-columns: 1fr;
    gap: var(--px-space-12);
    padding-block: var(--px-space-16);
  }

  .hero__copy {
    max-width: 640px;
  }

  .hero__title {
    font-size: var(--px-font-size-h1);
    line-height: var(--px-line-heading);
    letter-spacing: var(--px-tracking-heading);
  }

  .hero__picture {
    max-width: 320px;
    margin-inline: auto;
  }
}

@media (max-width: 767px) {
  .hero__grid {
    padding-block: var(--px-space-12);
  }

  .hero__body {
    max-width: none;
  }
}

@media (max-width: 479px) {
  .hero__art {
    display: none;
  }

  .hero__actions {
    flex-direction: column;
    margin-top: var(--px-space-6);
  }

  .hero__action {
    width: 100%;
  }
}
</style>

<style>
/* Invert the oxblood ink to a warm light tone so the line art reads on the
   dark canvas without a plate or background behind it. */
:root[data-theme='dark'] .hero__image {
  filter: invert(1) hue-rotate(180deg) saturate(1.15) brightness(0.94);
}
</style>
