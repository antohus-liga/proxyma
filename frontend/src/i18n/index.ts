import { createI18n } from 'vue-i18n'
import { applyDocumentLocale, resolveInitialLocale } from './locale'
import enGB from './locales/en-GB'
import ptPT from './locales/pt-PT'

const initialLocale = resolveInitialLocale()

export const i18n = createI18n({
  legacy: false,
  locale: initialLocale,
  fallbackLocale: 'en-GB',
  messages: {
    'en-GB': enGB,
    'pt-PT': ptPT,
  },
})

applyDocumentLocale(initialLocale)
