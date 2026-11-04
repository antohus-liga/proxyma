<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterView } from 'vue-router'
import AccountSectionNav from '@/components/account/AccountSectionNav.vue'
import AppShell from '@/components/layout/AppShell.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'

const { t } = useI18n()
</script>

<template>
  <AppShell context="account">
    <PxPageHeader :title="t('account.title')" :description="t('account.description')" />

    <div class="account-layout">
      <AccountSectionNav />
      <div class="account-layout__content">
        <RouterView />
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.account-layout {
  display: grid;
  grid-template-columns: 192px minmax(0, 1fr);
  gap: var(--px-space-8);
  align-items: start;
}

.account-layout__content {
  width: 100%;
  max-width: var(--px-form-max);
  min-width: 0;
}

@media (max-width: 1023px) {
  .account-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--px-space-5);
  }
}
</style>
