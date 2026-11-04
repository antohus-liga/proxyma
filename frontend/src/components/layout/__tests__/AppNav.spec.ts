import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import enGB from '@/i18n/locales/en-GB'
import AppNav from '../AppNav.vue'
import { accountNavItems } from '../nav'

const StubView = { template: '<div />' }

async function mountNav(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/app', name: 'app', component: StubView },
      { path: '/app/password', name: 'account-password', component: StubView },
      { path: '/admin', name: 'admin', component: StubView },
      { path: '/admin/users', name: 'admin-users', component: StubView },
    ],
  })

  await router.push(path)
  await router.isReady()

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })

  return mount(AppNav, {
    props: {
      items: accountNavItems,
      label: 'Account',
    },
    global: {
      plugins: [router, i18n],
    },
  })
}

describe('AppNav', () => {
  it('marks Account as current on the profile route', async () => {
    const wrapper = await mountNav('/app')

    expect(wrapper.get('a[href="/app"]').attributes('aria-current')).toBe('page')
  })

  it('keeps Account current on the password route', async () => {
    const wrapper = await mountNav('/app/password')

    expect(wrapper.get('a[href="/app"]').attributes('aria-current')).toBe('page')
    expect(wrapper.get('a[href="/app"]').classes()).toContain('app-nav__link--active')
  })
})
