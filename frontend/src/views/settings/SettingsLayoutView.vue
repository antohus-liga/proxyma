<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import { RouterView } from 'vue-router'
import AppShell from '@/components/layout/AppShell.vue'
import SettingsSectionNav from '@/components/settings/SettingsSectionNav.vue'
import PxPageHeader from '@/components/ui/PxPageHeader.vue'

const { t } = useI18n()
</script>

<template>
  <AppShell context="settings">
    <PxPageHeader :title="t('settings.title')" :description="t('settings.description')" />

    <div class="settings-layout">
      <SettingsSectionNav />
      <div class="settings-layout__content">
        <RouterView />
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.settings-layout {
  display: grid;
  grid-template-columns: 192px minmax(0, 1fr);
  gap: var(--px-space-8);
  align-items: start;
}

.settings-layout__content {
  width: 100%;
  max-width: var(--px-form-max);
  min-width: 0;
}

@media (max-width: 1023px) {
  .settings-layout {
    grid-template-columns: minmax(0, 1fr);
    gap: var(--px-space-5);
  }
}
</style>
