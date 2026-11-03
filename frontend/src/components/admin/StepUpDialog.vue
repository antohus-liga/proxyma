<script setup lang="ts">
import { reactive, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PxButton from '@/components/ui/PxButton.vue'
import PxDialog from '@/components/ui/PxDialog.vue'
import PxField from '@/components/ui/PxField.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPasswordInput from '@/components/ui/PxPasswordInput.vue'
import PxTextarea from '@/components/ui/PxTextarea.vue'

const props = withDefaults(
  defineProps<{
    title: string
    description: string
    confirmLabel: string
    busy?: boolean
    requiresPassword?: boolean
    error?: string | null
  }>(),
  {
    busy: false,
    requiresPassword: true,
    error: null,
  },
)

const emit = defineEmits<{
  confirm: [payload: { reason: string; password?: string }]
}>()

const open = defineModel<boolean>('open', { default: false })
const { t } = useI18n()

const form = reactive({
  reason: '',
  password: '',
})
const fieldErrors = reactive<{ reason?: string; password?: string }>({})

watch(open, (isOpen) => {
  if (!isOpen) {
    return
  }

  form.reason = ''
  form.password = ''
  fieldErrors.reason = undefined
  fieldErrors.password = undefined
})

function validate(): boolean {
  fieldErrors.reason = form.reason.trim() ? undefined : t('admin.stepUp.reasonRequired')
  fieldErrors.password =
    props.requiresPassword && !form.password ? t('admin.stepUp.passwordRequired') : undefined

  return !fieldErrors.reason && !fieldErrors.password
}

function onSubmit() {
  if (props.busy || !validate()) {
    return
  }

  emit('confirm', {
    reason: form.reason.trim(),
    ...(props.requiresPassword ? { password: form.password } : {}),
  })
}
</script>

<template>
  <PxDialog
    v-model:open="open"
    :title="title"
    :description="description"
    size="sm"
    :close-label="t('common.close')"
    :busy="busy"
  >
    <form class="step-up" novalidate @submit.prevent="onSubmit">
      <PxNotice v-if="error" tone="danger" role="alert">
        {{ error }}
      </PxNotice>

      <PxField
        :label="t('admin.stepUp.reason')"
        :help="t('admin.stepUp.reasonHelp')"
        :error="fieldErrors.reason"
        required
      >
        <template #default="{ id, describedBy, invalid }">
          <PxTextarea
            :id="id"
            v-model="form.reason"
            :aria-describedby="describedBy"
            :invalid="invalid"
            :maxlength="500"
            :rows="3"
            autofocus
          />
        </template>
      </PxField>

      <PxField
        v-if="requiresPassword"
        :label="t('admin.stepUp.password')"
        :error="fieldErrors.password"
        required
      >
        <template #default="{ id, describedBy, invalid }">
          <PxPasswordInput
            :id="id"
            v-model="form.password"
            autocomplete="current-password"
            :described-by="describedBy"
            :invalid="invalid"
            required
          />
        </template>
      </PxField>

      <div class="step-up__warning">
        <PxNotice tone="warning">
          {{ t('admin.stepUp.warning') }}
        </PxNotice>
      </div>

      <div class="step-up__actions">
        <PxButton variant="secondary" :disabled="busy" @click="open = false">
          {{ t('common.cancel') }}
        </PxButton>
        <PxButton type="submit" variant="danger" :loading="busy">
          {{ confirmLabel }}
        </PxButton>
      </div>
    </form>
  </PxDialog>
</template>

<style scoped>
.step-up {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-4);
}

.step-up__warning {
  margin-block-start: var(--px-space-2);
}

.step-up__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--px-space-2);
  margin-block-start: var(--px-space-2);
  padding-block-start: var(--px-space-4);
  border-block-start: var(--px-border-width) solid var(--px-color-border-subtle);
}

@media (max-width: 479px) {
  .step-up__actions {
    flex-direction: column-reverse;
  }

  .step-up__actions :deep(.px-button) {
    width: 100%;
  }
}
</style>
