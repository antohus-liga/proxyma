<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import PxButton from '@/components/ui/PxButton.vue'
import PxField from '@/components/ui/PxField.vue'
import PxNotice from '@/components/ui/PxNotice.vue'
import PxPanel from '@/components/ui/PxPanel.vue'
import PxPasswordInput from '@/components/ui/PxPasswordInput.vue'
import { useRetryCountdown } from '@/composables/useRetryCountdown'
import { asApiError } from '@/lib/api'
import {
  mapPasswordError,
  type PasswordField,
  type PasswordFieldErrors,
} from '@/lib/account-errors'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const errors = reactive<PasswordFieldErrors>({})
const formError = ref<string | null>(null)
const submitting = ref(false)
const formAlert = ref<HTMLElement | null>(null)

const {
  seconds: retrySeconds,
  isBlocked: retryBlocked,
  start: startRetryCountdown,
} = useRetryCountdown()

watch(retrySeconds, (seconds) => {
  if (seconds === 0 && formError.value === 'errors.RATE_LIMITED') {
    formError.value = null
  }
})

function validateCurrentPassword() {
  if (!form.currentPassword) {
    errors.currentPassword = 'account.password.fieldErrors.currentRequired'
    return false
  }

  delete errors.currentPassword
  return true
}

function validateNewPassword() {
  if (!form.newPassword) {
    errors.newPassword = 'account.password.fieldErrors.newRequired'
    return false
  }

  if (form.newPassword.length < 10) {
    errors.newPassword = 'account.password.fieldErrors.tooShort'
    return false
  }

  if (form.newPassword.length > 128) {
    errors.newPassword = 'account.password.fieldErrors.tooLong'
    return false
  }

  delete errors.newPassword

  if (form.confirmPassword) {
    validateConfirmPassword()
  }

  return true
}

function validateConfirmPassword() {
  if (!form.confirmPassword) {
    errors.confirmPassword = 'account.password.fieldErrors.confirmRequired'
    return false
  }

  if (form.confirmPassword !== form.newPassword) {
    errors.confirmPassword = 'account.password.fieldErrors.mismatch'
    return false
  }

  delete errors.confirmPassword
  return true
}

const fieldValidators: Record<PasswordField, () => boolean> = {
  currentPassword: validateCurrentPassword,
  newPassword: validateNewPassword,
  confirmPassword: validateConfirmPassword,
}

function validateForm() {
  const currentValid = validateCurrentPassword()
  const newValid = validateNewPassword()
  const confirmValid = validateConfirmPassword()

  return currentValid && newValid && confirmValid
}

function onBlur(field: PasswordField) {
  // Keep server-side field errors until the value changes.
  if (errors[field]?.startsWith('errors.')) {
    return
  }

  fieldValidators[field]()
}

function onInput(field: PasswordField) {
  if (errors[field]) {
    fieldValidators[field]()
  }
}

function focusFirstInvalid() {
  const id = errors.currentPassword
    ? 'account-current-password'
    : errors.newPassword
      ? 'account-new-password'
      : errors.confirmPassword
        ? 'account-confirm-password'
        : null

  if (id) {
    document.getElementById(id)?.focus()
  }
}

async function onSubmit() {
  formError.value = null

  if (!validateForm()) {
    await nextTick()
    focusFirstInvalid()
    return
  }

  const email = auth.user?.email
  submitting.value = true

  try {
    await auth.changePassword(form.currentPassword, form.newPassword)

    await router.replace({
      name: 'sign-in',
      query: email ? { reason: 'password-changed', email } : { reason: 'password-changed' },
    })
  } catch (cause) {
    const mapped = mapPasswordError(asApiError(cause))

    if (mapped.fieldErrors.currentPassword === 'errors.REAUTH_FAILED') {
      form.currentPassword = ''
    }

    Object.assign(errors, mapped.fieldErrors)
    formError.value = mapped.formError ?? null

    if (mapped.retryAfterSeconds !== undefined) {
      startRetryCountdown(mapped.retryAfterSeconds)
    }

    await nextTick()

    if (errors.currentPassword || errors.newPassword || errors.confirmPassword) {
      focusFirstInvalid()
    } else {
      formAlert.value?.focus()
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <PxPanel :title="t('account.password.title')" :description="t('account.password.description')">
    <div class="account-password">
      <PxNotice tone="warning" :title="t('account.password.warning.title')">
        {{ t('account.password.warning.description') }}
      </PxNotice>

      <form class="account-form" novalidate @submit.prevent="onSubmit">
        <div
          v-if="formError"
          ref="formAlert"
          class="account-form__focus-target"
          tabindex="-1"
        >
          <PxNotice tone="danger">
            {{ t(formError, { seconds: retrySeconds }) }}
          </PxNotice>
        </div>

        <PxField
          :label="t('account.password.currentPassword.label')"
          input-id="account-current-password"
          required
          :error="errors.currentPassword ? t(errors.currentPassword) : undefined"
        >
          <template #default="{ id, describedBy, invalid }">
            <PxPasswordInput
              :id="id"
              v-model="form.currentPassword"
              autocomplete="current-password"
              :invalid="invalid"
              :described-by="describedBy"
              @blur="onBlur('currentPassword')"
              @input="onInput('currentPassword')"
            />
          </template>
        </PxField>

        <PxField
          :label="t('account.password.newPassword.label')"
          input-id="account-new-password"
          required
          :help="t('account.password.newPassword.help')"
          :error="errors.newPassword ? t(errors.newPassword) : undefined"
        >
          <template #default="{ id, describedBy, invalid }">
            <PxPasswordInput
              :id="id"
              v-model="form.newPassword"
              autocomplete="new-password"
              :maxlength="128"
              :invalid="invalid"
              :described-by="describedBy"
              @blur="onBlur('newPassword')"
              @input="onInput('newPassword')"
            />
          </template>
        </PxField>

        <PxField
          :label="t('account.password.confirmPassword.label')"
          input-id="account-confirm-password"
          required
          :error="errors.confirmPassword ? t(errors.confirmPassword) : undefined"
        >
          <template #default="{ id, describedBy, invalid }">
            <PxPasswordInput
              :id="id"
              v-model="form.confirmPassword"
              autocomplete="new-password"
              :maxlength="128"
              :invalid="invalid"
              :described-by="describedBy"
              @blur="onBlur('confirmPassword')"
              @input="onInput('confirmPassword')"
            />
          </template>
        </PxField>

        <div class="account-form__actions">
          <PxButton type="submit" :loading="submitting" :disabled="retryBlocked">
            {{ t('account.password.submit') }}
          </PxButton>
        </div>
      </form>
    </div>
  </PxPanel>
</template>

<style scoped>
.account-password,
.account-form {
  display: flex;
  flex-direction: column;
}

.account-password {
  gap: var(--px-space-5);
}

.account-form {
  gap: var(--px-space-5);
}

.account-form__focus-target {
  margin-block-end: calc(-1 * var(--px-space-2));
}

.account-form__focus-target:focus {
  outline: none;
  box-shadow: none;
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
