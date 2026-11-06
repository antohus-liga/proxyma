import { mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { createI18n } from 'vue-i18n'
import { LOCALE_STORAGE_KEY } from '@/i18n/locale'
import enGB from '@/i18n/locales/en-GB'
import ptPT from '@/i18n/locales/pt-PT'
import LocaleControl from '../LocaleControl.vue'

function mountControl(locale: 'en-GB' | 'pt-PT' = 'en-GB') {
  const i18n = createI18n({
    legacy: false,
    locale,
    fallbackLocale: 'en-GB',
    messages: {
      'en-GB': enGB,
      'pt-PT': ptPT,
    },
  })

  return {
    i18n,
    wrapper: mount(LocaleControl, {
      global: { plugins: [i18n] },
    }),
  }
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  document.documentElement.lang = 'en-GB'
})

describe('LocaleControl', () => {
  it('lists the registered locales', () => {
    const { wrapper } = mountControl()
    const options = wrapper.findAll('option')

    expect(options.map((option) => option.text())).toEqual(['English (UK)', 'Português (Portugal)'])
  })

  it('switches, persists, and updates the document language', async () => {
    const { wrapper, i18n } = mountControl()

    await wrapper.get('select').setValue('pt-PT')

    expect(i18n.global.locale.value).toBe('pt-PT')
    expect(localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('pt-PT')
    expect(document.documentElement.lang).toBe('pt-PT')
    expect(wrapper.get('.locale-control__label').text()).toBe('Idioma')
  })
})
