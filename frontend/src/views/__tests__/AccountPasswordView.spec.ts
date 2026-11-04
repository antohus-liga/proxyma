import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import enGB from '@/i18n/locales/en-GB'
import { accountApi, type ChangePasswordInput, type UpdateProfileInput } from '@/lib/account-api'
import { api, ApiError, type ApiProblem, type User } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'
import AccountPasswordView from '../account/AccountPasswordView.vue'
import { createTestRouter } from './test-router'

vi.mock('@/lib/api', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@/lib/api')>()

  return {
    ...actual,
    api: {
      ...actual.api,
      logout: vi.fn<typeof actual.api.logout>(),
    },
  }
})

vi.mock('@/lib/account-api', () => ({
  accountApi: {
    updateProfile: vi.fn<(input: UpdateProfileInput) => Promise<User>>(),
    changePassword: vi.fn<(input: ChangePasswordInput) => Promise<void>>(),
  },
}))

const mockedAccountApi = vi.mocked(accountApi)
const mockedLogout = vi.mocked(api.logout)

function problem(code: string, path?: string): ApiProblem {
  return {
    type: 'about:blank',
    title: 'Request failed',
    status: 400,
    detail: 'Request failed',
    instance: '/api/v1/users/me/password',
    code,
    ...(path ? { errors: [{ path, message: 'invalid' }] } : {}),
  }
}

async function mountPassword() {
  const pinia = createPinia()
  setActivePinia(pinia)

  const auth = useAuthStore()
  auth.user = {
    id: 'user-1',
    email: 'ana@example.com',
    display_name: 'Ana',
    is_platform_admin: false,
  }
  auth.accessToken = 'access-token'
  auth.status = 'authenticated'

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })
  const router = createTestRouter({ 'account-password': AccountPasswordView })

  await router.push('/app/password')
  await router.isReady()

  const wrapper = mount(AccountPasswordView, {
    global: {
      plugins: [pinia, router, i18n],
    },
  })

  return { wrapper, auth, router }
}

async function fillForm(
  wrapper: Awaited<ReturnType<typeof mountPassword>>['wrapper'],
  values: { current: string; next: string; confirm: string },
) {
  await wrapper.get('input[autocomplete="current-password"]').setValue(values.current)

  const newPasswordInputs = wrapper.findAll('input[autocomplete="new-password"]')
  await newPasswordInputs[0]!.setValue(values.next)
  await newPasswordInputs[1]!.setValue(values.confirm)
}

describe('AccountPasswordView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockedLogout.mockResolvedValue(undefined)
    setActivePinia(createPinia())
  })

  it('warns that changing the password signs the user out everywhere', async () => {
    const { wrapper } = await mountPassword()

    expect(wrapper.text()).toContain('You will be signed out')
    expect(wrapper.text()).toContain('signs you out of every device, including this one')
  })

  it('validates all three fields before calling the API', async () => {
    const { wrapper } = await mountPassword()

    await wrapper.get('form').trigger('submit')

    expect(mockedAccountApi.changePassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Enter your current password.')
    expect(wrapper.text()).toContain('Enter a new password.')
    expect(wrapper.text()).toContain('Confirm your new password.')
  })

  it('rejects a confirmation that does not match', async () => {
    const { wrapper } = await mountPassword()

    await fillForm(wrapper, {
      current: 'old-password',
      next: 'new-password-123',
      confirm: 'different-password',
    })
    await wrapper.get('form').trigger('submit')

    expect(mockedAccountApi.changePassword).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('The passwords do not match.')
  })

  it('maps a rejected current password to the current-password field', async () => {
    mockedAccountApi.changePassword.mockRejectedValue(
      new ApiError({ code: 'REAUTH_FAILED', status: 401 }),
    )
    const { wrapper } = await mountPassword()

    await fillForm(wrapper, {
      current: 'wrong-password',
      next: 'new-password-123',
      confirm: 'new-password-123',
    })
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('That password is incorrect. Try again.')
    expect(wrapper.get('input[autocomplete="current-password"]').element).toHaveProperty('value', '')
  })

  it('maps a weak new password to the new-password field', async () => {
    mockedAccountApi.changePassword.mockRejectedValue(
      new ApiError({
        code: 'WEAK_PASSWORD',
        status: 422,
        problem: problem('WEAK_PASSWORD', '/new_password'),
      }),
    )
    const { wrapper } = await mountPassword()

    await fillForm(wrapper, {
      current: 'old-password',
      next: 'new-password-123',
      confirm: 'new-password-123',
    })
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Choose a stronger password.')
  })

  it('clears the session and returns to sign-in after a password change', async () => {
    mockedAccountApi.changePassword.mockResolvedValue(undefined)
    const { wrapper, auth, router } = await mountPassword()

    await fillForm(wrapper, {
      current: 'old-password',
      next: 'new-password-123',
      confirm: 'new-password-123',
    })
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedAccountApi.changePassword).toHaveBeenCalledWith({
      current_password: 'old-password',
      new_password: 'new-password-123',
    })
    expect(mockedLogout).toHaveBeenCalled()
    expect(auth.accessToken).toBeNull()
    expect(auth.user).toBeNull()
    expect(auth.passwordChanged).toBe(true)
    expect(router.currentRoute.value.name).toBe('sign-in')
    expect(router.currentRoute.value.query).toEqual({
      reason: 'password-changed',
      email: 'ana@example.com',
    })
  })
})
