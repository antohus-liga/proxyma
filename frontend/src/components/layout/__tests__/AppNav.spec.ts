import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import enGB from '@/i18n/locales/en-GB'
import AppNav from '../AppNav.vue'
import { settingsNavItems } from '../nav'

const StubView = { template: '<div />' }

async function mountNav(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/settings', name: 'settings', component: StubView },
      { path: '/settings/account', name: 'settings-account', component: StubView },
      { path: '/settings/general', name: 'settings-general', component: StubView },
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
      items: settingsNavItems,
      label: 'Settings',
    },
    global: {
      plugins: [router, i18n],
    },
  })
}

describe('AppNav', () => {
  it('marks Settings as current on the settings route', async () => {
    const wrapper = await mountNav('/settings')

    expect(wrapper.get('a[href="/settings"]').attributes('aria-current')).toBe('page')
  })

  it('keeps Settings current on nested settings routes', async () => {
    const wrapper = await mountNav('/settings/general')

    expect(wrapper.get('a[href="/settings"]').attributes('aria-current')).toBe('page')
    expect(wrapper.get('a[href="/settings"]').classes()).toContain('app-nav__link--active')
  })
})
