import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import enGB from '@/i18n/locales/en-GB'
import { useAuthStore } from '@/stores/auth'
import AccountView from '../AccountView.vue'
import { createTestRouter } from './test-router'

const AppShellStub = {
  template: '<div><slot /></div>',
}

async function mountAccount(isAdmin = false) {
  const pinia = createPinia()
  setActivePinia(pinia)

  const auth = useAuthStore()
  auth.user = {
    id: 'user-1',
    email: 'ana@example.com',
    display_name: 'Ana',
    is_platform_admin: isAdmin,
  }
  auth.accessToken = 'access-token'
  auth.status = 'authenticated'

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })
  const router = createTestRouter({ app: AccountView })

  await router.push('/app')
  await router.isReady()

  const wrapper = mount(AccountView, {
    global: {
      plugins: [pinia, router, i18n],
      stubs: { AppShell: AppShellStub },
    },
  })

  return wrapper
}

describe('AccountView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders the signed-in account details and the honest notice', async () => {
    const wrapper = await mountAccount()

    expect(wrapper.get('h1').text()).toBe('Your account')
    expect(wrapper.text()).toContain('Ana')
    expect(wrapper.text()).toContain('ana@example.com')
    expect(wrapper.text()).toContain('Standard account')
    expect(wrapper.text()).toContain("Organisation and proxy features aren't available yet.")
    expect(wrapper.find('a[href="/admin"]').exists()).toBe(false)
  })

  it('shows the admin entry only for platform admins', async () => {
    const wrapper = await mountAccount(true)

    expect(wrapper.text()).toContain('Platform administrator')
    expect(wrapper.find('a[href="/admin"]').text()).toBe('Open platform administration')
  })
})
