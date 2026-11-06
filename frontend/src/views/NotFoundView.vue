<script setup lang="ts">
import NotFoundContent from '@/components/layout/NotFoundContent.vue'
</script>

<template>
  <main id="main-content" class="not-found" tabindex="-1">
    <NotFoundContent />
  </main>
</template>

<style scoped>
.not-found {
  display: grid;
  min-height: 100dvh;
  align-content: center;
  padding: var(--px-page-padding-sm);
}

.not-found :deep(.not-found-content) {
  margin-inline: auto;
  padding-block: 0;
}
</style>
