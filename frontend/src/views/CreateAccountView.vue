<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import AuthCard from '@/components/auth/AuthCard.vue'
import AuthNotice from '@/components/auth/AuthNotice.vue'
import PxButton from '@/components/ui/PxButton.vue'
import PxField from '@/components/ui/PxField.vue'
import PxInput from '@/components/ui/PxInput.vue'
import PxPasswordInput from '@/components/ui/PxPasswordInput.vue'
import { useRetryCountdown } from '@/composables/useRetryCountdown'
import { asApiError } from '@/lib/api'
import { mapAuthError, type AuthField, type AuthFieldErrors } from '@/lib/auth-errors'
import { buildAuthPath, safeRedirect } from '@/lib/redirect'
import { useAuthStore } from '@/stores/auth'

const { t } = useI18n()
const route = useRoute()
const auth = useAuthStore()

const initialEmail = typeof route.query.email === 'string' ? route.query.email : ''
const form = reactive({
  displayName: '',
  email: initialEmail,
  password: '',
})
const errors = reactive<AuthFieldErrors>({})
const formError = ref<string | null>(null)
const submitting = ref(false)
const created = ref(false)
const createdEmail = ref('')
const formAlert = ref<HTMLElement | null>(null)

const {
  seconds: retrySeconds,
  isBlocked: retryBlocked,
  start: startRetryCountdown,
} = useRetryCountdown()
const redirectTarget = computed(() => safeRedirect(route.query.redirect))
const signInPath = computed(() =>
  buildAuthPath('/sign-in', {
    email: form.email.trim() || undefined,
    redirect: redirectTarget.value,
  }),
)
const successSignInPath = computed(() =>
  buildAuthPath('/sign-in', {
    email: createdEmail.value,
    redirect: redirectTarget.value,
  }),
)

const title = computed(() =>
  created.value ? t('auth.createAccount.success.title') : t('auth.createAccount.title'),
)
const description = computed(() =>
  created.value
    ? t('auth.createAccount.success.description')
    : t('auth.createAccount.description'),
)

watch(retrySeconds, (seconds) => {
  if (seconds === 0 && formError.value === 'errors.RATE_LIMITED') {
    formError.value = null
  }
})

function validateDisplayName() {
  const displayName = form.displayName.trim()

  if (!displayName) {
    errors.displayName = 'auth.fieldErrors.displayName.required'
    return false
  }

  if (displayName.length > 80) {
    errors.displayName = 'auth.fieldErrors.displayName.tooLong'
    return false
  }

  delete errors.displayName
  return true
}

function validateEmail() {
  const email = form.email.trim()

  if (!email) {
    errors.email = 'auth.fieldErrors.email.required'
    return false
  }

  if (email.length > 254) {
    errors.email = 'auth.fieldErrors.email.tooLong'
    return false
  }

  delete errors.email
  return true
}

function validatePassword() {
  if (!form.password) {
    errors.password = 'auth.fieldErrors.password.required'
    return false
  }

  if (form.password.length < 10) {
    errors.password = 'auth.fieldErrors.password.tooShort'
    return false
  }

  if (form.password.length > 128) {
    errors.password = 'auth.fieldErrors.password.tooLong'
    return false
  }

  delete errors.password
  return true
}

function validateForm() {
  const displayNameValid = validateDisplayName()
  const emailValid = validateEmail()
  const passwordValid = validatePassword()
  return displayNameValid && emailValid && passwordValid
}

const fieldValidators: Record<AuthField, () => boolean> = {
  displayName: validateDisplayName,
  email: validateEmail,
  password: validatePassword,
}

function onBlur(field: AuthField) {
  // Keep server-side field errors until the value changes. Clearing them on
  // blur would also remove inline help (such as the sign-in link) when the
  // user moves focus to that help.
  if (errors[field]?.startsWith('errors.')) {
    return
  }

  fieldValidators[field]()
}

function onInput(field: AuthField) {
  if (errors[field]) {
    fieldValidators[field]()
  }
}

function focusFirstInvalid() {
  const id = errors.displayName
    ? 'create-account-display-name'
    : errors.email
      ? 'create-account-email'
      : errors.password
        ? 'create-account-password'
        : null

  if (id) {
    document.getElementById(id)?.focus()
  }
}

function focusHeading() {
  document.querySelector<HTMLElement>('#main-content h1')?.focus()
}

async function onSubmit() {
  formError.value = null

  if (!validateForm()) {
    await nextTick()
    focusFirstInvalid()
    return
  }

  submitting.value = true

  try {
    await auth.register({
      email: form.email.trim(),
      display_name: form.displayName.trim(),
      password: form.password,
    })

    createdEmail.value = form.email.trim()
    created.value = true
    await nextTick()
    focusHeading()
  } catch (cause) {
    const mapped = mapAuthError(asApiError(cause))

    Object.assign(errors, mapped.fieldErrors)
    formError.value = mapped.suspended
      ? 'errors.ACCOUNT_SUSPENDED'
      : (mapped.formError ?? null)

    if (mapped.retryAfterSeconds !== undefined) {
      startRetryCountdown(mapped.retryAfterSeconds)
    }

    await nextTick()

    if (errors.displayName || errors.email || errors.password) {
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
  <AuthCard :title="title" :description="description">
    <div v-if="created" class="auth-form__success">
      <PxButton :to="successSignInPath" size="lg" full-width>
        {{ t('auth.createAccount.success.signIn') }}
      </PxButton>
    </div>

    <form v-else class="auth-form" novalidate @submit.prevent="onSubmit">
      <div v-if="formError" ref="formAlert" class="auth-form__focus-target" tabindex="-1">
        <AuthNotice tone="danger">
          {{ t(formError, { seconds: retrySeconds }) }}
        </AuthNotice>
      </div>

      <PxField
        :label="t('auth.fields.displayName')"
        input-id="create-account-display-name"
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
            @blur="onBlur('displayName')"
            @input="onInput('displayName')"
          />
        </template>
      </PxField>

      <PxField
        :label="t('auth.fields.email')"
        input-id="create-account-email"
        required
        :error="errors.email ? t(errors.email) : undefined"
      >
        <template #default="{ id, describedBy, invalid }">
          <PxInput
            :id="id"
            v-model="form.email"
            type="email"
            autocomplete="email"
            inputmode="email"
            autocapitalize="none"
            spellcheck="false"
            maxlength="254"
            :invalid="invalid"
            :aria-describedby="describedBy"
            @blur="onBlur('email')"
            @input="onInput('email')"
          />
        </template>
        <template #error>
          <template v-if="errors.email === 'errors.EMAIL_TAKEN'">
            {{ t('auth.createAccount.emailTaken') }}
            <RouterLink :to="signInPath">
              {{ t('auth.createAccount.emailTakenLink') }}
            </RouterLink>.
          </template>
          <template v-else>{{ errors.email ? t(errors.email) : '' }}</template>
        </template>
      </PxField>

      <PxField
        :label="t('auth.fields.password')"
        input-id="create-account-password"
        required
        :help="t('auth.createAccount.passwordHelp')"
        :error="errors.password ? t(errors.password) : undefined"
      >
        <template #default="{ id, describedBy, invalid }">
          <PxPasswordInput
            :id="id"
            v-model="form.password"
            autocomplete="new-password"
            :invalid="invalid"
            :described-by="describedBy"
            @blur="onBlur('password')"
            @input="onInput('password')"
          />
        </template>
      </PxField>

      <PxButton
        class="auth-form__submit"
        type="submit"
        size="lg"
        full-width
        :loading="submitting"
        :disabled="retryBlocked"
      >
        {{ t('auth.createAccount.submit') }}
      </PxButton>
    </form>

    <template #footer>
      <span v-if="!created">
        {{ t('auth.createAccount.haveAccount') }}
        <RouterLink :to="signInPath">
          {{ t('auth.createAccount.signInLink') }}
        </RouterLink>
      </span>
    </template>
  </AuthCard>
</template>

<style scoped>
.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-4);
}

.auth-form__focus-target {
  margin-bottom: var(--px-space-4);
}

.auth-form__focus-target:focus {
  outline: none;
  box-shadow: none;
}

.auth-form__submit {
  margin-top: var(--px-space-2);
}

.auth-form__success {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-4);
}
</style>
