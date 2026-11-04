<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute, useRouter } from 'vue-router'
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
const router = useRouter()
const auth = useAuthStore()

const initialEmail = typeof route.query.email === 'string' ? route.query.email : ''
const form = reactive({
  email: initialEmail,
  password: '',
})
const errors = reactive<AuthFieldErrors>({})
const formError = ref<string | null>(null)
const submitting = ref(false)
const suspended = ref(route.query.reason === 'suspended')
const formAlert = ref<HTMLElement | null>(null)
const suspendedAlert = ref<HTMLElement | null>(null)

const {
  seconds: retrySeconds,
  isBlocked: retryBlocked,
  start: startRetryCountdown,
} = useRetryCountdown()
const sessionExpired = computed(
  () => route.query.reason === 'session-expired' || auth.sessionExpired,
)
const passwordChanged = computed(() => route.query.reason === 'password-changed')
const redirectTarget = computed(() => safeRedirect(route.query.redirect))
const createAccountPath = computed(() =>
  buildAuthPath('/create-account', { redirect: redirectTarget.value }),
)

watch(retrySeconds, (seconds) => {
  if (seconds === 0 && formError.value === 'errors.RATE_LIMITED') {
    formError.value = null
  }
})

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

  delete errors.password
  return true
}

function validateForm() {
  const emailValid = validateEmail()
  const passwordValid = validatePassword()
  return emailValid && passwordValid
}

function onBlur(field: AuthField) {
  // Keep server-side field errors until the value changes. Clearing them on
  // blur would discard validation feedback the user has not addressed yet.
  if (errors[field]?.startsWith('errors.')) {
    return
  }

  if (field === 'email') {
    validateEmail()
  }

  if (field === 'password') {
    validatePassword()
  }
}

function onInput(field: AuthField) {
  if (!errors[field]) {
    return
  }

  if (field === 'email') {
    validateEmail()
  }

  if (field === 'password') {
    validatePassword()
  }
}

function focusFirstInvalid() {
  const id = errors.email ? 'sign-in-email' : errors.password ? 'sign-in-password' : null

  if (id) {
    document.getElementById(id)?.focus()
  }
}

function focusFormAlert() {
  formAlert.value?.focus()
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
    await auth.signIn(form.email.trim(), form.password)
    await router.replace(redirectTarget.value ?? '/app')
  } catch (cause) {
    const mapped = mapAuthError(asApiError(cause))

    Object.assign(errors, mapped.fieldErrors)
    formError.value = mapped.formError ?? null
    form.password = ''

    if (mapped.suspended) {
      suspended.value = true
    }

    if (mapped.retryAfterSeconds !== undefined) {
      startRetryCountdown(mapped.retryAfterSeconds)
    }

    await nextTick()

    if (suspended.value) {
      suspendedAlert.value?.focus()
    } else if (errors.email || errors.password) {
      focusFirstInvalid()
    } else {
      focusFormAlert()
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <AuthCard
    :title="t('auth.signIn.title')"
    :description="t('auth.signIn.description')"
  >
    <div v-if="passwordChanged && !suspended" class="auth-form__notice">
      <AuthNotice tone="success">{{ t('auth.signIn.passwordChanged') }}</AuthNotice>
    </div>

    <div v-if="sessionExpired && !suspended" class="auth-form__notice">
      <AuthNotice tone="info">{{ t('auth.signIn.sessionExpired') }}</AuthNotice>
    </div>

    <div v-if="suspended" ref="suspendedAlert" class="auth-form__focus-target" tabindex="-1">
      <AuthNotice tone="danger">{{ t('errors.ACCOUNT_SUSPENDED') }}</AuthNotice>
    </div>

    <form v-else class="auth-form" novalidate @submit.prevent="onSubmit">
      <div v-if="formError" ref="formAlert" class="auth-form__focus-target" tabindex="-1">
        <AuthNotice tone="danger">
          {{ t(formError, { seconds: retrySeconds }) }}
        </AuthNotice>
      </div>

      <PxField
        :label="t('auth.fields.email')"
        input-id="sign-in-email"
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
      </PxField>

      <PxField
        :label="t('auth.fields.password')"
        input-id="sign-in-password"
        required
        :error="errors.password ? t(errors.password) : undefined"
      >
        <template #default="{ id, describedBy, invalid }">
          <PxPasswordInput
            :id="id"
            v-model="form.password"
            autocomplete="current-password"
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
        {{ t('auth.signIn.submit') }}
      </PxButton>
    </form>

    <template #footer>
      <span v-if="!suspended">
        {{ t('auth.signIn.newHere') }}
        <RouterLink :to="createAccountPath">
          {{ t('auth.signIn.createAccountLink') }}
        </RouterLink>
      </span>
    </template>
  </AuthCard>
</template>

<style scoped>
.auth-form__notice,
.auth-form__focus-target {
  margin-bottom: var(--px-space-4);
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: var(--px-space-4);
}

.auth-form__submit {
  margin-top: var(--px-space-2);
}

.auth-form__focus-target:focus {
  outline: none;
  box-shadow: none;
}
</style>
