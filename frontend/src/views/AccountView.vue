<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ShieldCheck } from '@lucide/vue'
import AppShell from '@/components/layout/AppShell.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()

const isAdmin = computed(() => auth.user?.is_platform_admin === true)
</script>

<template>
  <AppShell context="account">
    <PxPageHeader :title="t('account.title')" :description="t('account.description')" />

    <div class="px-page-stack">
      <PxNotice tone="info">
        {{ t('account.notice') }}
      </PxNotice>

      <PxPanel :title="t('account.summary.title')" :description="t('account.summary.description')">
        <dl class="px-definition-list">
          <dt>{{ t('account.summary.displayName') }}</dt>
          <dd>{{ auth.user?.display_name }}</dd>

          <dt>{{ t('account.summary.email') }}</dt>
          <dd>{{ auth.user?.email }}</dd>

          <dt>{{ t('account.summary.type') }}</dt>
          <dd>
            {{ isAdmin ? t('account.summary.admin') : t('account.summary.standard') }}
          </dd>
        </dl>
      </PxPanel>

      <PxPanel
        v-if="isAdmin"
        :title="t('account.admin.title')"
        :description="t('account.admin.description')"
      >
        <template #actions>
          <ShieldCheck :size="18" aria-hidden="true" />
        </template>

        <PxButton to="/admin" variant="secondary">
          {{ t('account.admin.open') }}
        </PxButton>
      </PxPanel>
    </div>
  </AppShell>
</template>
