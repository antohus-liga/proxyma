<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { FocusScope } from 'reka-ui'
import AppSidebar from './AppSidebar.vue'
import AppTopBar from './AppTopBar.vue'

const props = withDefaults(
  defineProps<{
    context?: 'account' | 'admin'
  }>(),
  {
    context: 'account',
  },
)

const { t } = useI18n()
const sidebarId = 'app-sidebar'
const drawerOpen = ref(false)
const isMobile = ref(false)
const previousFocus = ref<HTMLElement | null>(null)

let mediaQuery: MediaQueryList | null = null
let previousOverflow = ''

function handleMediaChange(event: MediaQueryListEvent | MediaQueryList) {
  isMobile.value = event.matches

  if (!event.matches) {
    closeDrawer()
  }
}

function openDrawer() {
  previousFocus.value = document.activeElement as HTMLElement | null
  drawerOpen.value = true
}

function closeDrawer() {
  if (!drawerOpen.value) {
    return
  }

  drawerOpen.value = false
  previousFocus.value?.focus()
  previousFocus.value = null
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && drawerOpen.value) {
    event.preventDefault()
    closeDrawer()
  }
}

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 767px)')
  handleMediaChange(mediaQuery)
  mediaQuery.addEventListener('change', handleMediaChange)
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  mediaQuery?.removeEventListener('change', handleMediaChange)
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = previousOverflow
})

watch(
  () => drawerOpen.value && isMobile.value,
  (locked) => {
    if (locked) {
      previousOverflow = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      return
    }

    document.body.style.overflow = previousOverflow
  },
)
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'app-shell--drawer-open': drawerOpen }"
    :data-context="props.context"
  >
    <FocusScope
      as-child
      :trapped="drawerOpen && isMobile"
      :present="!isMobile || drawerOpen"
      :loop="drawerOpen && isMobile"
    >
      <aside :id="sidebarId" class="app-shell__sidebar">
        <AppSidebar @navigate="closeDrawer" />
      </aside>
    </FocusScope>

    <button
      v-if="drawerOpen"
      type="button"
      class="app-shell__backdrop"
      :aria-label="t('nav.closeMenu')"
      @click="closeDrawer"
    />

    <div class="app-shell__main">
      <AppTopBar
        :context="props.context"
        :drawer-open="drawerOpen"
        :sidebar-id="sidebarId"
        @open-menu="openDrawer"
      />

      <main id="main-content" class="app-shell__content" tabindex="-1">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100dvh;
}

.app-shell__sidebar {
  position: fixed;
  inset-block-start: 0;
  inset-inline-start: 0;
  z-index: calc(var(--px-z-overlay) + 1);
  height: 100dvh;
}

.app-shell__main {
  display: flex;
  min-height: 100dvh;
  flex-direction: column;
  margin-inline-start: var(--px-sidebar-width);
}

.app-shell__content {
  width: 100%;
  max-width: var(--px-content-max);
  flex: 1;
  margin-inline: auto;
  padding: var(--px-space-8) var(--px-page-padding) var(--px-space-12);
}

.app-shell__content:focus {
  outline: none;
  box-shadow: none;
}

.app-shell__backdrop {
  display: none;
}

@media (max-width: 1023px) {
  .app-shell__content {
    padding-inline: var(--px-page-padding-md);
  }
}

@media (max-width: 767px) {
  .app-shell__sidebar {
    z-index: calc(var(--px-z-overlay) + 1);
    visibility: hidden;
    transform: translateX(-100%);
    transition:
      transform var(--px-duration-normal) var(--px-ease-out),
      visibility var(--px-duration-normal);
  }

  .app-shell__sidebar :deep(.app-sidebar) {
    width: min(320px, 88vw);
  }

  .app-shell--drawer-open .app-shell__sidebar {
    visibility: visible;
    transform: translateX(0);
  }

  .app-shell__main {
    margin-inline-start: 0;
  }

  .app-shell__content {
    padding: var(--px-space-6) var(--px-page-padding-sm) var(--px-space-10);
  }

  .app-shell__backdrop {
    position: fixed;
    inset: 0;
    z-index: var(--px-z-overlay);
    display: block;
    width: 100%;
    height: 100%;
    padding: 0;
    border: 0;
    background: var(--px-color-overlay);
    cursor: pointer;
  }
}
</style>
