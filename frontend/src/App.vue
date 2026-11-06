<script setup lang="ts">
import { watch } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

watch(
  () => auth.isAuthenticated,
  (isAuthenticated) => {
    if (isAuthenticated || !route.meta.requiresAuth) {
      return
    }

    const query: Record<string, string> = { redirect: route.fullPath }

    if (auth.suspended) {
      query.reason = 'suspended'
    } else if (auth.sessionExpired) {
      query.reason = 'session-expired'
    } else if (auth.passwordChanged) {
      // The password view navigates with its own reason and preserved email.
      return
    }

    void router.replace({ name: 'sign-in', query })
  },
)
</script>

<template>
  <a class="px-skip-link" href="#main-content">{{ t('a11y.skipToContent') }}</a>
  <RouterView />
</template>
