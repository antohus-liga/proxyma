import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createI18n } from 'vue-i18n'
import enGB from '@/i18n/locales/en-GB'
import { api, ApiError, type ApiProblem, type LoginResponse } from '@/lib/api'
import SignInView from '../SignInView.vue'
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

function problem(code: string, errors?: ApiProblem['errors']): ApiProblem {
  return {
    type: 'about:blank',
    title: 'Error',
    status: 422,
    detail: 'Request failed',
    instance: '/api/v1/auth/login',
    code,
    errors,
  }
}

async function mountSignIn(path = '/sign-in') {
  const pinia = createPinia()
  setActivePinia(pinia)

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })

  const router = createTestRouter({ 'sign-in': SignInView })

  await router.push(path)
  await router.isReady()

  const wrapper = mount(SignInView, {
    global: { plugins: [pinia, router, i18n] },
  })

  return { wrapper, router }
}

async function submit(wrapper: Awaited<ReturnType<typeof mountSignIn>>['wrapper']) {
  await wrapper.get('form').trigger('submit')
  await flushPromises()
}

describe('SignInView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders the sign-in form and the create-account link', async () => {
    const { wrapper } = await mountSignIn()

    expect(wrapper.get('h1').text()).toBe('Sign in to Proxyma')
    expect(wrapper.get('label[for="sign-in-email"]').text()).toContain('Email')
    expect(wrapper.get('label[for="sign-in-password"]').text()).toContain('Password')
    expect(wrapper.get('a[href="/create-account"]').text()).toBe('Create an account')
  })

  it('prefills the email from the query string', async () => {
    const { wrapper } = await mountSignIn('/sign-in?email=ana%40example.com')

    expect((wrapper.get('#sign-in-email').element as HTMLInputElement).value).toBe(
      'ana@example.com',
    )
  })

  it('shows an informational message after a session expires', async () => {
    const { wrapper } = await mountSignIn('/sign-in?reason=session-expired')

    expect(wrapper.text()).toContain('Your session has ended. Sign in again.')
  })

  it('shows the password-changed notice and keeps the email', async () => {
    const { wrapper } = await mountSignIn(
      '/sign-in?reason=password-changed&email=ana%40example.com',
    )

    expect(wrapper.text()).toContain('Password changed. Sign in with your new password.')
    expect((wrapper.get('#sign-in-email').element as HTMLInputElement).value).toBe(
      'ana@example.com',
    )
  })

  it('submits credentials and navigates to the app placeholder', async () => {
    mockedApi.login.mockResolvedValue(loginResponse)
    const { wrapper, router } = await mountSignIn()

    await wrapper.get('#sign-in-email').setValue('ana@example.com')
    await wrapper.get('#sign-in-password').setValue('correct horse battery staple')
    await submit(wrapper)

    expect(mockedApi.login).toHaveBeenCalledWith({
      email: 'ana@example.com',
      password: 'correct horse battery staple',
    })
    expect(router.currentRoute.value.path).toBe('/app')
  })

  it('keeps the email and clears the password after invalid credentials', async () => {
    mockedApi.login.mockRejectedValue(
      new ApiError({
        code: 'INVALID_CREDENTIALS',
        problem: problem('INVALID_CREDENTIALS'),
      }),
    )
    const { wrapper } = await mountSignIn()

    await wrapper.get('#sign-in-email').setValue('ana@example.com')
    await wrapper.get('#sign-in-password').setValue('wrong password')
    await submit(wrapper)

    expect(wrapper.text()).toContain('Email or password is incorrect.')
    expect((wrapper.get('#sign-in-email').element as HTMLInputElement).value).toBe(
      'ana@example.com',
    )
    expect((wrapper.get('#sign-in-password').element as HTMLInputElement).value).toBe('')
  })

  it('keeps submission enabled after a server-side validation error', async () => {
    mockedApi.login.mockRejectedValue(
      new ApiError({
        code: 'INVALID_CREDENTIALS',
        problem: problem('INVALID_CREDENTIALS'),
        retryAfterSeconds: 60,
      }),
    )
    const { wrapper } = await mountSignIn()

    await wrapper.get('#sign-in-email').setValue('ana@example.com')
    await wrapper.get('#sign-in-password').setValue('wrong password')
    await submit(wrapper)

    expect(wrapper.text()).toContain('Email or password is incorrect.')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeUndefined()
  })

  it('replaces the form when the account is suspended', async () => {
    mockedApi.login.mockRejectedValue(
      new ApiError({
        code: 'ACCOUNT_SUSPENDED',
        problem: problem('ACCOUNT_SUSPENDED'),
      }),
    )
    const { wrapper } = await mountSignIn()

    await wrapper.get('#sign-in-email').setValue('ana@example.com')
    await wrapper.get('#sign-in-password').setValue('correct horse battery staple')
    await submit(wrapper)

    expect(wrapper.find('form').exists()).toBe(false)
    expect(wrapper.text()).toContain('This account is suspended.')
    expect(wrapper.find('a[href="/create-account"]').exists()).toBe(false)
  })

  it('disables submission during the rate-limit window', async () => {
    mockedApi.login.mockRejectedValue(
      new ApiError({
        code: 'RATE_LIMITED',
        problem: problem('RATE_LIMITED'),
        retryAfterSeconds: 5,
      }),
    )
    const { wrapper } = await mountSignIn()

    await wrapper.get('#sign-in-email').setValue('ana@example.com')
    await wrapper.get('#sign-in-password').setValue('correct horse battery staple')
    await submit(wrapper)

    expect(wrapper.text()).toContain('Too many attempts. Try again in 5 seconds.')
    expect(wrapper.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    wrapper.unmount()
  })

  it('validates required fields before calling the API', async () => {
    const { wrapper } = await mountSignIn()

    await submit(wrapper)

    expect(wrapper.text()).toContain('Enter your email address.')
    expect(wrapper.text()).toContain('Enter your password.')
    expect(mockedApi.login).not.toHaveBeenCalled()
  })

  it('toggles password visibility', async () => {
    const { wrapper } = await mountSignIn()
    const toggle = wrapper.get('.px-password__toggle')
    const input = wrapper.get('#sign-in-password')

    expect(input.attributes('type')).toBe('password')
    expect(toggle.text()).toBe('Show')

    await toggle.trigger('click')

    expect(input.attributes('type')).toBe('text')
    expect(toggle.text()).toBe('Hide')
  })
})
