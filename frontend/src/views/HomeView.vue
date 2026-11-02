<script setup lang="ts">
import SiteHeader from '@/components/layout/SiteHeader.vue'
import SiteFooter from '@/components/layout/SiteFooter.vue'
import HomeHero from '@/components/home/HomeHero.vue'
import HomeValueProps from '@/components/home/HomeValueProps.vue'
import HomeHowItWorks from '@/components/home/HomeHowItWorks.vue'
import HomeClosingAction from '@/components/home/HomeClosingAction.vue'
</script>

<template>
  <div class="home">
    <SiteHeader />

    <main id="main-content" class="home__main" tabindex="-1">
      <HomeHero />
      <HomeValueProps />
      <HomeHowItWorks />
      <HomeClosingAction />
    </main>

    <SiteFooter />
  </div>
</template>

<style scoped>
.home {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
}

.home__main {
  flex: 1;
}
</style>
