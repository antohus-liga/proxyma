import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import enGB from '@/i18n/locales/en-GB'
import { accountApi, type ChangePasswordInput, type UpdateProfileInput } from '@/lib/account-api'
import { ApiError, type ApiProblem, type User } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'
import SettingsAccountView from '../settings/SettingsAccountView.vue'
import { createTestRouter } from './test-router'

vi.mock('@/lib/account-api', () => ({
  accountApi: {
    updateProfile: vi.fn<(input: UpdateProfileInput) => Promise<User>>(),
    changePassword: vi.fn<(input: ChangePasswordInput) => Promise<void>>(),
  },
}))

const mockedAccountApi = vi.mocked(accountApi)

const user: User = {
  id: 'user-1',
  email: 'ana@example.com',
  display_name: 'Ana',
  is_platform_admin: false,
}

function problem(code: string, path?: string): ApiProblem {
  return {
    type: 'about:blank',
    title: 'Request failed',
    status: 422,
    detail: 'Request failed',
    instance: '/api/v1/users/me',
    code,
    ...(path ? { errors: [{ path, message: 'invalid' }] } : {}),
  }
}

async function mountProfile(initialUser: User = user) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const auth = useAuthStore()
  auth.user = initialUser
  auth.accessToken = 'access-token'
  auth.status = 'authenticated'

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })
  const router = createTestRouter({ 'settings-account': SettingsAccountView })

  await router.push('/settings/account')
  await router.isReady()

  const wrapper = mount(SettingsAccountView, {
    global: {
      plugins: [pinia, router, i18n],
    },
  })

  return { wrapper, auth }
}

describe('SettingsAccountView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('renders the editable name and account type', async () => {
    const { wrapper } = await mountProfile()

    expect(wrapper.get('input[autocomplete="name"]').element).toHaveProperty('value', 'Ana')
    expect(wrapper.text()).toContain('ana@example.com')
    expect(wrapper.text()).toContain('Standard account')
    expect(wrapper.find('input[type="email"]').exists()).toBe(false)
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()
  })

  it('saves a changed display name and confirms success', async () => {
    const updated: User = { ...user, display_name: 'Ana Smith' }
    mockedAccountApi.updateProfile.mockResolvedValue(updated)
    const { wrapper, auth } = await mountProfile()

    await wrapper.get('input[autocomplete="name"]').setValue('Ana Smith')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()

    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mockedAccountApi.updateProfile).toHaveBeenCalledWith({ display_name: 'Ana Smith' })
    expect(auth.user).toEqual(updated)
    expect(wrapper.text()).toContain('Your profile has been updated.')
  })

  it('rejects an empty display name before calling the API', async () => {
    const { wrapper } = await mountProfile()

    await wrapper.get('input[autocomplete="name"]').setValue('   ')
    await wrapper.get('form').trigger('submit')

    expect(mockedAccountApi.updateProfile).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Enter a display name.')
  })

  it('maps a server validation error to the display-name field', async () => {
    mockedAccountApi.updateProfile.mockRejectedValue(
      new ApiError({
        code: 'VALIDATION_FAILED',
        status: 422,
        problem: problem('VALIDATION_FAILED', '/display_name'),
      }),
    )
    const { wrapper } = await mountProfile()

    await wrapper.get('input[autocomplete="name"]').setValue('Ana Smith')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('Enter a display name of 80 characters or fewer.')
  })

  it('shows the admin entry only for platform admins', async () => {
    const { wrapper } = await mountProfile({ ...user, is_platform_admin: true })

    expect(wrapper.text()).toContain('Platform administrator')
    expect(wrapper.get('a[href="/admin"]').text()).toBe('Open platform administration')
  })
})
