<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import LocaleControl from '@/components/layout/LocaleControl.vue'
import ThemeControl from '@/components/layout/ThemeControl.vue'
import PxPanel from '@/components/ui/PxPanel.vue'

const { t } = useI18n()
</script>

<template>
  <div class="px-page-stack">
    <PxPanel
      :title="t('settings.general.appearance.title')"
      :description="t('settings.general.appearance.description')"
    >
      <ThemeControl label-hidden />
    </PxPanel>

    <PxPanel
      :title="t('settings.general.language.title')"
      :description="t('settings.general.language.description')"
    >
      <LocaleControl label-hidden />
    </PxPanel>
  </div>
</template>
