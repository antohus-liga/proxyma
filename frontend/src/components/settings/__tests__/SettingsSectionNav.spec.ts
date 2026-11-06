import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { createMemoryHistory, createRouter } from 'vue-router'
import enGB from '@/i18n/locales/en-GB'
import SettingsSectionNav from '../SettingsSectionNav.vue'

const StubView = { template: '<div />' }

async function mountNav(path: string) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/settings/account', name: 'settings-account', component: StubView },
      { path: '/settings/general', name: 'settings-general', component: StubView },
      { path: '/settings/security', name: 'settings-security', component: StubView },
    ],
  })

  await router.push(path)
  await router.isReady()

  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })

  return mount(SettingsSectionNav, {
    global: { plugins: [router, i18n] },
  })
}

describe('SettingsSectionNav', () => {
  it('lists the settings sections', async () => {
    const wrapper = await mountNav('/settings/account')

    expect(wrapper.findAll('a').map((link) => link.text())).toEqual([
      'Account',
      'General',
      'Security',
    ])
  })

  it.each([
    ['/settings/account', 'Account'],
    ['/settings/general', 'General'],
    ['/settings/security', 'Security'],
  ])('marks %s as current', async (path, label) => {
    const wrapper = await mountNav(path)

    expect(wrapper.get(`a[href="${path}"]`).text()).toBe(label)
    expect(wrapper.get(`a[href="${path}"]`).attributes('aria-current')).toBe('page')
  })
})
