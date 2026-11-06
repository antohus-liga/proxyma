<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Menu, ShieldCheck } from '@lucide/vue'
import PxIconButton from '@/components/ui/PxIconButton.vue'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  context: 'settings' | 'admin'
  drawerOpen: boolean
  sidebarId: string
}>()

const emit = defineEmits<{
  'open-menu': []
}>()

const { t } = useI18n()
const auth = useAuthStore()

const initials = computed(() => auth.user?.display_name.trim().charAt(0).toUpperCase() ?? '?')
</script>

<template>
  <header class="app-topbar">
    <PxIconButton
      class="app-topbar__menu"
      :label="t('nav.openMenu')"
      :aria-controls="sidebarId"
      :aria-expanded="drawerOpen ? 'true' : 'false'"
      @click="emit('open-menu')"
    >
      <Menu :size="20" aria-hidden="true" />
    </PxIconButton>

    <div class="app-topbar__context">
      <span v-if="props.context === 'admin'" class="app-topbar__context-label">
        <ShieldCheck :size="15" aria-hidden="true" />
        {{ t('nav.adminSection') }}
      </span>
      <span v-else class="app-topbar__context-label">{{ t('nav.settings') }}</span>
    </div>

    <div v-if="auth.user" class="app-topbar__account">
      <span class="app-topbar__avatar" aria-hidden="true">{{ initials }}</span>
      <span class="app-topbar__identity">
        <span class="app-topbar__name">{{ auth.user.display_name }}</span>
        <span class="app-topbar__email">{{ auth.user.email }}</span>
      </span>
    </div>
  </header>
</template>

<style scoped>
.app-topbar {
  position: sticky;
  inset-block-start: 0;
  z-index: var(--px-z-sticky);
  display: flex;
  min-height: var(--px-topbar-height);
  align-items: center;
  gap: var(--px-space-3);
  padding-inline: var(--px-space-6);
  border-block-end: var(--px-border-width) solid var(--px-color-border-subtle);
  background: var(--px-color-canvas);
}

.app-topbar__menu {
  display: none;
}

.app-topbar__context {
  min-width: 0;
  flex: 1;
}

.app-topbar__context-label {
  display: inline-flex;
  align-items: center;
  gap: var(--px-space-1);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.app-topbar__account {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: var(--px-space-2);
}

.app-topbar__avatar {
  display: inline-flex;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: var(--px-radius-full);
  background: var(--px-color-surface-subtle);
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-caption);
  font-weight: var(--px-font-weight-semibold);
  line-height: 1;
}

.app-topbar__identity {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.app-topbar__name {
  overflow: hidden;
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-small);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-topbar__email {
  overflow: hidden;
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 767px) {
  .app-topbar {
    padding-inline: var(--px-space-4);
  }

  .app-topbar__menu {
    display: inline-flex;
  }

  .app-topbar__email {
    display: none;
  }
}
</style>
