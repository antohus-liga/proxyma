<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import PxBadge from '@/components/ui/PxBadge.vue'
import type { AdminConfigStatus, AdminOrgState, AdminUserStatus } from '@/lib/admin-api'
import type { BadgeTone } from '@/components/ui/types'

type StatusKind = 'user' | 'org' | 'config'

const props = defineProps<{
  kind: StatusKind
  value: AdminUserStatus | AdminOrgState | AdminConfigStatus
}>()

const { t } = useI18n()

const badge = computed<{ tone: BadgeTone; labelKey: string }>(() => {
  if (props.kind === 'user') {
    return props.value === 'suspended'
      ? { tone: 'danger', labelKey: 'admin.status.user.suspended' }
      : { tone: 'success', labelKey: 'admin.status.user.active' }
  }

  if (props.kind === 'config') {
    if (props.value === 'applied') {
      return { tone: 'success', labelKey: 'admin.status.config.applied' }
    }

    if (props.value === 'rejected') {
      return { tone: 'danger', labelKey: 'admin.status.config.rejected' }
    }

    return { tone: 'warning', labelKey: 'admin.status.config.pending' }
  }

  switch (props.value) {
    case 'ONLINE':
      return { tone: 'success', labelKey: 'admin.status.org.ONLINE' }
    case 'STARTING':
      return { tone: 'warning', labelKey: 'admin.status.org.STARTING' }
    case 'OFFLINE':
      return { tone: 'danger', labelKey: 'admin.status.org.OFFLINE' }
    case 'DELETING':
      return { tone: 'neutral', labelKey: 'admin.status.org.DELETING' }
    default:
      return { tone: 'neutral', labelKey: 'admin.status.org.CREATING' }
  }
})
</script>

<template>
  <PxBadge :tone="badge.tone" dot>{{ t(badge.labelKey) }}</PxBadge>
</template>
