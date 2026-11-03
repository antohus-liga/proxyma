import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import enGB from '@/i18n/locales/en-GB'
import { api, ApiError, type ApiProblem, type User } from '@/lib/api'
import CreateAccountView from '../CreateAccountView.vue'
import { createTestRouter } from './test-router'

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

const createdUser: User = {
  id: 'user-1',
  email: 'ana@example.com',
  display_name: 'Ana',
  is_platform_admin: false,
}

function problem(code: string, errors?: ApiProblem['errors']): ApiProblem {
  return {
    type: 'about:blank',
    title: 'Error',
    status: 422,
    detail: 'Request failed',
    instance: '/api/v1/auth/register',
    code,
    errors,
  }
}

async function mountCreateAccount(path = '/create-account') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })

  const router = createTestRouter({ 'create-account': CreateAccountView })

  await router.push(path)
  await router.isReady()

  const wrapper = mount(CreateAccountView, {
    global: { plugins: [pinia, router, i18n] },
  })

  return { wrapper, router }
}

async function fillValidForm(wrapper: Awaited<ReturnType<typeof mountCreateAccount>>['wrapper']) {
  await wrapper.get('#create-account-display-name').setValue('Ana')
  await wrapper.get('#create-account-email').setValue('ana@example.com')
  await wrapper.get('#create-account-password').setValue('correct horse battery staple')
}

async function submit(wrapper: Awaited<ReturnType<typeof mountCreateAccount>>['wrapper']) {
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('CreateAccountView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the registration form with password guidance', async () => {
    const { wrapper } = await mountCreateAccount()

    expect(wrapper.get('h1').text()).toBe('Create your Proxyma account')
    expect(wrapper.get('label[for="create-account-display-name"]').text()).toContain('Display name')
    expect(wrapper.get('label[for="create-account-email"]').text()).toContain('Email')
    expect(wrapper.get('label[for="create-account-password"]').text()).toContain('Password')
    expect(wrapper.text()).toContain('Use at least 10 characters. Avoid common passwords.')
    expect(wrapper.get('a[href="/sign-in"]').text()).toBe('Sign in')
  })

  it('registers and replaces the form with a success state', async () => {
    mockedApi.register.mockResolvedValue(createdUser)
    const { wrapper } = await mountCreateAccount()

    await fillValidForm(wrapper)
    await submit(wrapper)

    expect(mockedApi.register).toHaveBeenCalledWith({
      email: 'ana@example.com',
      display_name: 'Ana',
      password: 'correct horse battery staple',
    })
    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.get('h1').text()).toBe('Account created')
    expect(wrapper.text()).toContain('Sign in to continue.')

    const link = wrapper.get('.auth-form__success a')
    expect(link.text()).toBe('Sign in')
    expect(link.attributes('href')).toBe('/sign-in?email=ana%40example.com')
  })

  it('preserves the redirect target in the success link', async () => {
    mockedApi.register.mockResolvedValue(createdUser)
    const { wrapper } = await mountCreateAccount(
      '/create-account?redirect=%2Fapp%2Fprojects%3Fpage%3D2',
    )

    await fillValidForm(wrapper)
    await submit(wrapper)

    expect(wrapper.get('.auth-form__success a').attributes('href')).toBe(
      '/sign-in?email=ana%40example.com&redirect=%2Fapp%2Fprojects%3Fpage%3D2',
    )
  })

  it('links an existing account to sign-in with the email filled in', async () => {
    mockedApi.register.mockRejectedValue(
      new ApiError({ code: 'EMAIL_TAKEN', problem: problem('EMAIL_TAKEN') }),
    )
    const { wrapper } = await mountCreateAccount()

    await fillValidForm(wrapper)
    await submit(wrapper)

    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.text()).toContain('An account with this email already exists.')
    const link = wrapper.get('#create-account-email-error a')
    expect(link.text()).toBe('Sign in instead')
    expect(link.attributes('href')).toBe('/sign-in?email=ana%40example.com')
  })

  it('shows a weak password against the password field', async () => {
    mockedApi.register.mockRejectedValue(
      new ApiError({ code: 'WEAK_PASSWORD', problem: problem('WEAK_PASSWORD') }),
    )
    const { wrapper } = await mountCreateAccount()

    await fillValidForm(wrapper)
    await submit(wrapper)

    expect(wrapper.text()).toContain('Choose a stronger password.')
    expect(wrapper.get('#create-account-password-error').text()).toContain(
      'Choose a stronger password.',
    )
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('validates required fields and password length before calling the API', async () => {
    const { wrapper } = await mountCreateAccount()

    await submit(wrapper)

    expect(wrapper.text()).toContain('Enter a display name.')
    expect(wrapper.text()).toContain('Enter your email address.')
    expect(wrapper.text()).toContain('Enter your password.')
    expect(mockedApi.register).not.toHaveBeenCalled()

    await wrapper.get('#create-account-display-name').setValue('Ana')
    await wrapper.get('#create-account-email').setValue('ana@example.com')
    await wrapper.get('#create-account-password').setValue('short')
    await submit(wrapper)

    expect(wrapper.text()).toContain('Use at least 10 characters.')
    expect(mockedApi.register).not.toHaveBeenCalled()
  })

  it('disables submission during the rate-limit window', async () => {
    mockedApi.register.mockRejectedValue(
      new ApiError({
        code: 'RATE_LIMITED',
        problem: problem('RATE_LIMITED'),
        retryAfterSeconds: 5,
      }),
    )
    const { wrapper } = await mountCreateAccount()

    await fillValidForm(wrapper)
    await submit(wrapper)

    expect(wrapper.text()).toContain('Too many attempts. Try again in 5 seconds.')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    wrapper.unmount()
  })

  it('toggles password visibility', async () => {
    const { wrapper } = await mountCreateAccount()
    const toggle = wrapper.get('.px-password__toggle')
    const input = wrapper.get('#create-account-password')

    expect(input.attributes('type')).toBe('password')
    expect(toggle.text()).toBe('Show')

    await toggle.trigger('click')

    expect(input.attributes('type')).toBe('text')
    expect(toggle.text()).toBe('Hide')
  })
})
