import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  api,
  asApiError,
  type ApiError,
  type LoginResponse,
  type RefreshOutcome,
  type RegisterInput,
  type User,
} from '@/lib/api'
import { accountApi } from '@/lib/account-api'

type AuthStatus = 'idle' | 'loading' | 'authenticated' | 'anonymous'

const REFRESH_LEEWAY_SECONDS = 60
const REFRESH_RETRY_SECONDS = 10
const RETRYABLE_REFRESH_CODES = new Set([
  'NETWORK_ERROR',
  'TIMEOUT',
  'INTERNAL_ERROR',
  'UNKNOWN',
])

function delay(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}

function retryDelayMs() {
  return 250 + Math.floor(Math.random() * 500)
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(null)
  const user = ref<User | null>(null)
  const expiresAt = ref<number | null>(null)
  const status = ref<AuthStatus>('idle')
  const suspended = ref(false)
  const sessionExpired = ref(false)
  const passwordChanged = ref(false)

  let refreshPromise: Promise<RefreshOutcome> | null = null
  let bootstrapPromise: Promise<void> | null = null
  let refreshTimer: ReturnType<typeof setTimeout> | null = null

  const isAuthenticated = computed(() => Boolean(accessToken.value && user.value))

  function clearRefreshTimer() {
    if (refreshTimer !== null) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  function clearSession() {
    clearRefreshTimer()
    accessToken.value = null
    user.value = null
    expiresAt.value = null
  }

  function scheduleRefresh(expiresIn: number) {
    clearRefreshTimer()

    if (!accessToken.value) {
      return
    }

    const delaySeconds = Math.max(5, expiresIn - REFRESH_LEEWAY_SECONDS)
    refreshTimer = setTimeout(() => {
      void refresh()
    }, delaySeconds * 1000)
  }

  function scheduleRefreshRetry(seconds = REFRESH_RETRY_SECONDS) {
    clearRefreshTimer()

    if (!accessToken.value) {
      return
    }

    refreshTimer = setTimeout(() => {
      void refresh()
    }, Math.max(1, seconds) * 1000)
  }

  function applySession(response: LoginResponse) {
    accessToken.value = response.access_token
    user.value = response.user
    expiresAt.value = Date.now() + response.expires_in * 1000
    status.value = 'authenticated'
    suspended.value = false
    sessionExpired.value = false
    passwordChanged.value = false
    scheduleRefresh(response.expires_in)
  }

  function handleRefreshFailure(error: ApiError, hadSession: boolean): RefreshOutcome {
    if (error.code === 'ACCOUNT_SUSPENDED') {
      suspended.value = true
      clearSession()
      status.value = 'anonymous'
      return 'failed'
    }

    if (error.code === 'INVALID_REFRESH_TOKEN' || error.code === 'UNAUTHORIZED') {
      if (hadSession) {
        sessionExpired.value = true
      }

      clearSession()
      status.value = 'anonymous'
      return 'failed'
    }

    if (error.code === 'RATE_LIMITED') {
      if (hadSession) {
        scheduleRefreshRetry(error.retryAfterSeconds ?? REFRESH_RETRY_SECONDS)
        status.value = 'authenticated'
        return 'deferred'
      }

      status.value = 'anonymous'
      return 'failed'
    }

    if (error.code === 'REFRESH_IN_PROGRESS') {
      if (hadSession) {
        scheduleRefreshRetry()
        status.value = 'authenticated'
        return 'deferred'
      }

      status.value = 'anonymous'
      return 'failed'
    }

    if (hadSession && RETRYABLE_REFRESH_CODES.has(error.code)) {
      scheduleRefreshRetry()
      status.value = 'authenticated'
      return 'deferred'
    }

    if (!hadSession) {
      status.value = 'anonymous'
    }

    return 'failed'
  }

  async function doRefresh(): Promise<RefreshOutcome> {
    const hadSession = isAuthenticated.value

    for (let attempt = 0; attempt < 3; attempt += 1) {
      try {
        const response = await api.refresh()
        applySession(response)
        return 'refreshed'
      } catch (cause) {
        const error = asApiError(cause)

        if (error.code === 'REFRESH_IN_PROGRESS' && attempt < 2) {
          await delay(retryDelayMs())
          continue
        }

        return handleRefreshFailure(error, hadSession)
      }
    }

    return 'deferred'
  }

  function refresh(): Promise<RefreshOutcome> {
    if (!refreshPromise) {
      refreshPromise = doRefresh().finally(() => {
        refreshPromise = null
      })
    }

    return refreshPromise
  }

  function bootstrap(): Promise<void> {
    if (status.value !== 'idle') {
      return Promise.resolve()
    }

    if (!bootstrapPromise) {
      bootstrapPromise = refresh()
        .then(() => {
          if (status.value === 'idle') {
            status.value = isAuthenticated.value ? 'authenticated' : 'anonymous'
          }
        })
        .finally(() => {
          bootstrapPromise = null
        })
    }

    return bootstrapPromise
  }

  function markSuspended() {
    suspended.value = true
    sessionExpired.value = false
    passwordChanged.value = false
    clearSession()
    status.value = 'anonymous'
  }

  async function signIn(email: string, password: string): Promise<User> {
    status.value = 'loading'

    try {
      const response = await api.login({ email, password })
      applySession(response)
      return response.user
    } catch (error) {
      status.value = isAuthenticated.value ? 'authenticated' : 'anonymous'
      throw error
    }
  }

  function register(input: RegisterInput): Promise<User> {
    return api.register({
      email: input.email,
      display_name: input.display_name,
      password: input.password,
    })
  }

  async function updateProfile(displayName: string): Promise<User> {
    const updated = await accountApi.updateProfile({ display_name: displayName.trim() })
    user.value = updated
    return updated
  }

  async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await accountApi.changePassword({
      current_password: currentPassword,
      new_password: newPassword,
    })

    clearSession()
    status.value = 'anonymous'
    suspended.value = false
    sessionExpired.value = false
    passwordChanged.value = true

    // The password change already revoked the refresh token server-side. Clear
    // the stale cookie in the background so a reload does not try to use it.
    void api.logout().catch(() => {})
  }

  async function signOut(): Promise<void> {
    clearSession()
    status.value = 'anonymous'
    suspended.value = false
    sessionExpired.value = false
    passwordChanged.value = false

    try {
      await api.logout()
    } catch {
      // The local session is already gone. Logout is idempotent server-side.
    }
  }

  return {
    accessToken,
    user,
    expiresAt,
    status,
    suspended,
    sessionExpired,
    passwordChanged,
    isAuthenticated,
    bootstrap,
    refresh,
    markSuspended,
    signIn,
    register,
    updateProfile,
    changePassword,
    signOut,
  }
})
