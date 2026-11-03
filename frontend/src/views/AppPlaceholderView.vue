<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import AuthCard from '@/components/auth/AuthCard.vue'
import PxButton from '@/components/ui/PxButton.vue'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()
const signingOut = ref(false)

const description = computed(() =>
  t('app.signedIn.description', {
    name: auth.user?.display_name ?? '',
    email: auth.user?.email ?? '',
  }),
)

async function onSignOut() {
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
  <AuthCard :title="t('app.signedIn.title')" :description="description">
    <PxButton variant="secondary" size="lg" full-width :loading="signingOut" @click="onSignOut">
      {{ t('app.signedIn.signOut') }}
    </PxButton>
  </AuthCard>
</template>
