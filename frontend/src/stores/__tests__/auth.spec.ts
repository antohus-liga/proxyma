import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError, type LoginResponse } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/lib/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/api')>()

  return {
    ...actual,
    api: {
      register: vi.fn<typeof actual.api.register>(),
      login: vi.fn<typeof actual.api.login>(),
      refresh: vi.fn<typeof actual.api.refresh>(),
      logout: vi.fn<typeof actual.api.logout>(),
    },
  }
})

const mockedApi = vi.mocked(api)

const loginResponse: LoginResponse = {
  access_token: 'access-token',
  token_type: 'Bearer',
  expires_in: 900,
  user: {
    id: 'user-1',
    email: 'ana@example.com',
    display_name: 'Ana',
    is_platform_admin: false,
  },
}

function authError(code: string, retryAfterSeconds?: number) {
  return new ApiError({ code, retryAfterSeconds })
}

describe('auth store', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('keeps the access token in memory after sign-in', async () => {
    mockedApi.login.mockResolvedValue(loginResponse)
    const auth = useAuthStore()

    await auth.signIn('ana@example.com', 'correct horse battery staple')

    expect(mockedApi.login).toHaveBeenCalledWith({
      email: 'ana@example.com',
      password: 'correct horse battery staple',
    })
    expect(auth.accessToken).toBe('access-token')
    expect(auth.user?.display_name).toBe('Ana')
    expect(auth.status).toBe('authenticated')
  })

  it('shares one refresh call between concurrent callers', async () => {
    let resolveRefresh: (value: LoginResponse) => void = () => {}
    const pending = new Promise<LoginResponse>((resolve) => {
      resolveRefresh = resolve
    })
    mockedApi.refresh.mockReturnValue(pending)
    const auth = useAuthStore()

    const first = auth.refresh()
    const second = auth.refresh()

    expect(mockedApi.refresh).toHaveBeenCalledTimes(1)

    resolveRefresh(loginResponse)
    await Promise.all([first, second])

    expect(auth.isAuthenticated).toBe(true)
  })

  it('restores a session with the refresh cookie', async () => {
    mockedApi.refresh.mockResolvedValue(loginResponse)
    const auth = useAuthStore()

    await auth.bootstrap()

    expect(auth.status).toBe('authenticated')
    expect(auth.isAuthenticated).toBe(true)
  })

  it('treats a missing refresh cookie as anonymous, not expired', async () => {
    mockedApi.refresh.mockRejectedValue(authError('INVALID_REFRESH_TOKEN'))
    const auth = useAuthStore()

    await auth.bootstrap()

    expect(auth.status).toBe('anonymous')
    expect(auth.sessionExpired).toBe(false)
  })

  it('marks the session expired when an existing session cannot refresh', async () => {
    mockedApi.login.mockResolvedValue(loginResponse)
    mockedApi.refresh.mockRejectedValue(authError('INVALID_REFRESH_TOKEN'))
    const auth = useAuthStore()

    await auth.signIn('ana@example.com', 'correct horse battery staple')
    await auth.refresh()

    expect(auth.status).toBe('anonymous')
    expect(auth.sessionExpired).toBe(true)
  })

  it('marks a suspended account and clears the session', async () => {
    mockedApi.refresh.mockRejectedValue(authError('ACCOUNT_SUSPENDED'))
    const auth = useAuthStore()

    await auth.bootstrap()

    expect(auth.status).toBe('anonymous')
    expect(auth.suspended).toBe(true)
    expect(auth.accessToken).toBeNull()
  })

  it('retries a concurrent refresh after 409', async () => {
    mockedApi.refresh
      .mockRejectedValueOnce(authError('REFRESH_IN_PROGRESS'))
      .mockResolvedValueOnce(loginResponse)
    const auth = useAuthStore()

    const refreshing = auth.refresh()
    await vi.advanceTimersByTimeAsync(1000)
    await refreshing

    expect(mockedApi.refresh).toHaveBeenCalledTimes(2)
    expect(auth.isAuthenticated).toBe(true)
  })

  it('clears the local session even when logout fails', async () => {
    mockedApi.login.mockResolvedValue(loginResponse)
    mockedApi.logout.mockRejectedValue(authError('NETWORK_ERROR'))
    const auth = useAuthStore()

    await auth.signIn('ana@example.com', 'correct horse battery staple')
    await auth.signOut()

    expect(auth.accessToken).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.status).toBe('anonymous')
  })
})
