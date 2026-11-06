import { useI18n } from 'vue-i18n'
import { applyDocumentLocale, persistLocale, type SupportedLocale } from '@/i18n/locale'

export function useLocale() {
  const { locale } = useI18n()

  function setLocale(value: SupportedLocale) {
    locale.value = value
    applyDocumentLocale(value)
    persistLocale(value)
  }

  return {
    locale,
    setLocale,
  }
}
