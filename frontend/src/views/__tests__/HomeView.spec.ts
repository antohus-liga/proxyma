import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import { createI18n } from 'vue-i18n'
import enGB from '@/i18n/locales/en-GB'
import HomeView from '../HomeView.vue'
import { createTestRouter } from './test-router'

async function mountHome() {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    messages: { 'en-GB': enGB },
  })
  const router = createTestRouter({ home: HomeView })

  await router.push('/')
  await router.isReady()

  return mount(HomeView, {
    global: { plugins: [i18n, router] },
  })
}

describe('HomeView', () => {
  it('renders the approved hero heading', async () => {
    const wrapper = await mountHome()

    expect(wrapper.get('h1').text()).toBe('Always near, always here.')
  })

  it('links the actions to the auth routes', async () => {
    const wrapper = await mountHome()
    const hrefs = wrapper.findAll('a').map((link) => link.attributes('href'))

    expect(hrefs).toContain('/create-account')
    expect(hrefs).toContain('/sign-in')
  })
})
