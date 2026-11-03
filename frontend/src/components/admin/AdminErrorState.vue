<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import PxButton from '@/components/ui/PxButton.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import { useApiErrorMessage } from '@/composables/useApiErrorMessage'
import type { ApiError } from '@/lib/api'

const props = defineProps<{
  error: ApiError
}>()

defineEmits<{
  retry: []
}>()

const { t } = useI18n()
const { messageFor } = useApiErrorMessage()
</script>

<template>
  <PxNotice tone="danger" :title="t('admin.error.title')" role="alert">
    <p>{{ messageFor(props.error) }}</p>
    <PxButton class="admin-error__retry" variant="secondary" size="sm" @click="$emit('retry')">
      {{ t('admin.error.retry') }}
    </PxButton>
  </PxNotice>
</template>

<style scoped>
.admin-error__retry {
  margin-block-start: var(--px-space-3);
}
</style>
