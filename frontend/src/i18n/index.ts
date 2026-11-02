import { createI18n } from 'vue-i18n'
import enGB from './locales/en-GB'

export const i18n = createI18n({
  legacy: false,
  locale: 'en-GB',
  fallbackLocale: 'en-GB',
  messages: {
    'en-GB': enGB,
  },
})

document.documentElement.lang = 'en-GB'
