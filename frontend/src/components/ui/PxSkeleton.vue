<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    width?: string
    height?: string
    rounded?: boolean
  }>(),
  {
    width: '100%',
    height: '1em',
    rounded: false,
  },
)

const style = computed(() => ({
  width: props.width,
  height: props.height,
}))
</script>

<template>
  <span
    class="px-skeleton"
    :class="{ 'px-skeleton--rounded': rounded }"
    :style="style"
    aria-hidden="true"
  />
</template>

<style scoped>
.px-skeleton {
  display: block;
  border-radius: var(--px-radius-control);
  background: var(--px-color-skeleton);
  animation: px-skeleton-pulse 1.6s var(--px-ease-in-out) infinite;
}

.px-skeleton--rounded {
  border-radius: var(--px-radius-full);
}

@keyframes px-skeleton-pulse {
  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.6;
  }
}
</style>
