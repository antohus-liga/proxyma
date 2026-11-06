<script setup lang="ts">
withDefaults(
  defineProps<{
    caption: string
    hideCaption?: boolean
  }>(),
  {
    hideCaption: true,
  },
)
</script>

<template>
  <div class="px-table-wrap">
    <table class="px-table">
      <caption :class="{ 'px-visually-hidden': hideCaption }">{{ caption }}</caption>
      <slot />
    </table>
  </div>
</template>

<style scoped>
.px-table-wrap {
  width: 100%;
  overflow-x: auto;
}
</style>
