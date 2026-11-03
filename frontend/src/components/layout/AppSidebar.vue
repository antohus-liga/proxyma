<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRouter } from 'vue-router'
import { LogOut, ShieldCheck } from '@lucide/vue'
import PxButton from '@/components/ui/PxButton.vue'
import { useAuthStore } from '@/stores/auth'
import AppNav from './AppNav.vue'
import ThemeControl from './ThemeControl.vue'
import { accountNavItems, adminNavItems } from './nav'

const emit = defineEmits<{
  navigate: []
}>()

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const signingOut = ref(false)

const isAdmin = computed(() => auth.user?.is_platform_admin === true)

async function onSignOut() {
  if (signingOut.value) {
    return
  }

  signingOut.value = true

  try {
    await auth.signOut()
    await router.replace({ name: 'sign-in' })
  } finally {
    signingOut.value = false
  }
}
</script>

<template>
  <div class="app-sidebar">
    <div class="app-sidebar__header">
      <RouterLink
        class="app-sidebar__wordmark"
        to="/app"
        :aria-label="t('app.homeLabel')"
        @click="emit('navigate')"
      >
        {{ t('app.wordmark') }}
      </RouterLink>
    </div>

    <div class="app-sidebar__body">
      <AppNav
        :items="accountNavItems"
        :label="t('nav.accountSection')"
        @navigate="emit('navigate')"
      />

      <section v-if="isAdmin" class="app-sidebar__admin">
        <div class="app-sidebar__section-header">
          <h2 class="app-sidebar__section-title">
            <ShieldCheck class="app-sidebar__section-icon" :size="14" aria-hidden="true" />
            {{ t('nav.adminSection') }}
          </h2>
        </div>

        <AppNav
          :items="adminNavItems"
          :label="t('nav.adminSection')"
          @navigate="emit('navigate')"
        />
      </section>
    </div>

    <div class="app-sidebar__footer">
      <ThemeControl />

      <PxButton
        variant="ghost"
        size="sm"
        full-width
        :loading="signingOut"
        class="app-sidebar__sign-out"
        @click="onSignOut"
      >
        <template #icon>
          <LogOut :size="15" aria-hidden="true" />
        </template>
        {{ t('nav.signOut') }}
      </PxButton>
    </div>
  </div>
</template>

<style scoped>
.app-sidebar {
  display: flex;
  width: var(--px-sidebar-width);
  height: 100dvh;
  flex-direction: column;
  border-inline-end: var(--px-border-width) solid var(--px-color-border-subtle);
  background: var(--px-color-canvas);
}

.app-sidebar__header {
  padding: var(--px-space-4);
}

.app-sidebar__wordmark {
  display: inline-block;
  color: var(--px-color-text-primary);
  font-size: 20px;
  font-weight: var(--px-font-weight-semibold);
  line-height: 1;
  letter-spacing: -0.02em;
}

.app-sidebar__wordmark:hover {
  color: var(--px-color-text-primary);
  text-decoration: none;
}

.app-sidebar__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--px-space-2) var(--px-space-4) var(--px-space-6);
}

.app-sidebar__admin {
  margin-block-start: var(--px-space-6);
  padding-block-start: var(--px-space-4);
  border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

.app-sidebar__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--px-space-2);
  margin-block-end: var(--px-space-2);
  padding-inline: var(--px-space-1);
}

.app-sidebar__section-title {
  display: flex;
  align-items: center;
  gap: var(--px-space-1);
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  font-weight: var(--px-font-weight-semibold);
  line-height: var(--px-line-compact);
  letter-spacing: var(--px-tracking-label);
  text-transform: uppercase;
}

.app-sidebar__section-icon {
  flex-shrink: 0;
}

.app-sidebar__footer {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-3);
  padding: var(--px-space-4);
  border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

.app-sidebar__sign-out {
  justify-content: flex-start;
}
</style>
