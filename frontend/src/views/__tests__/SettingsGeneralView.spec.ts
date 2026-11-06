import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { LOCALE_STORAGE_KEY } from '@/i18n/locale'
import enGB from '@/i18n/locales/en-GB'
import ptPT from '@/i18n/locales/pt-PT'
import SettingsGeneralView from '../settings/SettingsGeneralView.vue'

function mountGeneral() {
  const i18n = createI18n({
    legacy: false,
    locale: 'en-GB',
    fallbackLocale: 'en-GB',
    messages: {
      'en-GB': enGB,
      'pt-PT': ptPT,
    },
  })

  return {
    i18n,
    wrapper: mount(SettingsGeneralView, {
      global: { plugins: [i18n] },
    }),
  }
}

beforeEach(() => {
  localStorage.clear()
  document.documentElement.dataset.theme = 'light'
  document.documentElement.lang = 'en-GB'
})

afterEach(() => {
  document.documentElement.dataset.theme = 'light'
  document.documentElement.lang = 'en-GB'
})

describe('SettingsGeneralView', () => {
  it('renders appearance and language panels', () => {
    const { wrapper } = mountGeneral()

    expect(wrapper.findAll('h2').map((heading) => heading.text())).toEqual([
      'Appearance',
      'Language',
    ])
    expect(
      wrapper
        .get('select')
        .findAll('option')
        .map((option) => option.text()),
    ).toEqual(['English (UK)', 'Português (Portugal)'])
  })

  it('changes and persists the theme', async () => {
    const { wrapper } = mountGeneral()

    await wrapper.get('button[aria-label="Use dark theme"]').trigger('click')

    expect(localStorage.getItem('proxyma.theme')).toBe('dark')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('changes the language and updates the settings copy', async () => {
    const { wrapper, i18n } = mountGeneral()

    await wrapper.get('select').setValue('pt-PT')

    expect(i18n.global.locale.value).toBe('pt-PT')
    expect(localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('pt-PT')
    expect(document.documentElement.lang).toBe('pt-PT')
    expect(wrapper.findAll('h2').map((heading) => heading.text())).toEqual(['Aparência', 'Idioma'])
  })
})
