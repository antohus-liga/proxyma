<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ShieldCheck } from '@lucide/vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxField from '@/components/ui/PxField.vue'
import PxInput from '@/components/ui/PxInput.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import { asApiError } from '@/lib/api'
import { mapProfileError, type ProfileFieldErrors } from '@/lib/account-errors'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const auth = useAuthStore()

const form = reactive({
  displayName: auth.user?.display_name ?? '',
})
const errors = reactive<ProfileFieldErrors>({})
const formError = ref<string | null>(null)
const success = ref(false)
const saving = ref(false)
const formAlert = ref<HTMLElement | null>(null)

const isAdmin = computed(() => auth.user?.is_platform_admin === true)
const accountType = computed(() =>
  isAdmin.value ? t('account.profile.type.admin') : t('account.profile.type.standard'),
)
const canSave = computed(
  () => !saving.value && form.displayName.trim() !== (auth.user?.display_name ?? ''),
)

function validateDisplayName() {
  const displayName = form.displayName.trim()

  if (!displayName) {
    errors.displayName = 'account.profile.fieldErrors.required'
    return false
  }

  if (displayName.length > 80) {
    errors.displayName = 'account.profile.fieldErrors.tooLong'
    return false
  }

  delete errors.displayName
  return true
}

function onBlur() {
  // Keep server-side field errors until the value changes.
  if (errors.displayName?.startsWith('errors.')) {
    return
  }

  validateDisplayName()
}

function onInput() {
  success.value = false
  formError.value = null

  if (errors.displayName) {
    validateDisplayName()
  }
}

async function onSubmit() {
  formError.value = null
  success.value = false

  if (!validateDisplayName()) {
    await nextTick()
    document.getElementById('account-display-name')?.focus()
    return
  }

  saving.value = true

  try {
    await auth.updateProfile(form.displayName)
    success.value = true
  } catch (cause) {
    const mapped = mapProfileError(asApiError(cause))

    Object.assign(errors, mapped.fieldErrors)
    formError.value = mapped.formError ?? null

    await nextTick()

    if (errors.displayName) {
      document.getElementById('account-display-name')?.focus()
    } else {
      formAlert.value?.focus()
    }
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="px-page-stack">
    <PxPanel :title="t('account.profile.title')" :description="t('account.profile.description')">
      <form class="account-form" novalidate @submit.prevent="onSubmit">
        <div
          v-if="formError"
          ref="formAlert"
          class="account-form__focus-target"
          tabindex="-1"
        >
          <PxNotice tone="danger">{{ t(formError) }}</PxNotice>
        </div>

        <PxNotice v-if="success" tone="success">
          {{ t('account.profile.success') }}
        </PxNotice>

        <PxField
          :label="t('account.profile.displayName.label')"
          input-id="account-display-name"
          :help="t('account.profile.displayName.help')"
          required
          :error="errors.displayName ? t(errors.displayName) : undefined"
        >
          <template #default="{ id, describedBy, invalid }">
            <PxInput
              :id="id"
              v-model="form.displayName"
              type="text"
              autocomplete="name"
              maxlength="80"
              :invalid="invalid"
              :aria-describedby="describedBy"
              @blur="onBlur"
              @input="onInput"
            />
          </template>
        </PxField>

        <div class="account-form__detail">
          <span class="account-form__detail-label">{{ t('account.profile.email.label') }}</span>
          <p class="account-form__detail-value">{{ auth.user?.email }}</p>
          <p class="account-form__detail-help">{{ t('account.profile.email.help') }}</p>
        </div>

        <div class="account-form__detail">
          <span class="account-form__detail-label">{{ t('account.profile.type.label') }}</span>
          <p class="account-form__detail-value">{{ accountType }}</p>
        </div>

        <div class="account-form__actions">
          <PxButton type="submit" :loading="saving" :disabled="!canSave">
            {{ t('account.profile.save') }}
          </PxButton>
        </div>
      </form>
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
</template>

<style scoped>
.account-form {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-5);
}

.account-form__focus-target {
  margin-block-end: calc(-1 * var(--px-space-2));
}

.account-form__focus-target:focus {
  outline: none;
  box-shadow: none;
}

.account-form__detail {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-1);
}

.account-form__detail-label {
  color: var(--px-color-text-primary);
  font-size: var(--px-font-size-small-strong);
  font-weight: var(--px-font-weight-medium);
  line-height: var(--px-line-compact);
}

.account-form__detail-value {
  color: var(--px-color-text-secondary);
  font-size: var(--px-font-size-small);
  line-height: var(--px-line-compact);
}

.account-form__detail-help {
  color: var(--px-color-text-tertiary);
  font-size: var(--px-font-size-caption);
  line-height: var(--px-line-compact);
}

.account-form__actions {
  display: flex;
  justify-content: flex-end;
  padding-block-start: var(--px-space-2);
}

@media (max-width: 767px) {
  .account-form__actions {
    justify-content: flex-start;
  }
}
</style>
