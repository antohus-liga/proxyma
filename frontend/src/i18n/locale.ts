export const SUPPORTED_LOCALES = ['en-GB', 'pt-PT'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const DEFAULT_LOCALE: SupportedLocale = 'en-GB'
export const LOCALE_STORAGE_KEY = 'proxyma.locale'

const BASE_LOCALE: Record<string, SupportedLocale> = {
  en: 'en-GB',
  pt: 'pt-PT',
}

export function isSupportedLocale(value: unknown): value is SupportedLocale {
  return typeof value === 'string' && (SUPPORTED_LOCALES as readonly string[]).includes(value)
}

function canonicalize(tag: string): string | null {
  try {
    return Intl.getCanonicalLocales(tag)[0] ?? null
  } catch {
    return null
  }
}

/**
 * Match a single BCP 47 tag against the supported catalogues.
 *
 * Exact matches win, then the base language. This lets `en-US` use the
 * `en-GB` catalogue and any Portuguese variant use `pt-PT` until a separate
 * `pt-BR` catalogue exists.
 */
export function matchLocale(tag: string): SupportedLocale | null {
  const canonical = canonicalize(tag)

  if (!canonical) {
    return null
  }

  const exact = SUPPORTED_LOCALES.find((locale) => locale.toLowerCase() === canonical.toLowerCase())

  if (exact) {
    return exact
  }

  const base = canonical.split('-')[0]?.toLowerCase()

  return base ? (BASE_LOCALE[base] ?? null) : null
}

/**
 * Choose the first supported locale from an ordered browser language list.
 */
export function detectLocale(languages: readonly string[]): SupportedLocale {
  for (const language of languages) {
    const match = matchLocale(language)

    if (match) {
      return match
    }
  }

  return DEFAULT_LOCALE
}

export function applyDocumentLocale(locale: string): void {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = locale
  }
}

export function readStoredLocale(): SupportedLocale | null {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    const stored = window.localStorage.getItem(LOCALE_STORAGE_KEY)

    return isSupportedLocale(stored) ? stored : null
  } catch {
    // Storage can be unavailable in private mode; use browser detection.
    return null
  }
}

export function persistLocale(locale: SupportedLocale): void {
  if (typeof window === 'undefined') {
    return
  }

  try {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, locale)
  } catch {
    // Keep the choice in memory when storage is unavailable.
  }
}

/**
 * Resolve the initial locale: an explicit stored choice first, then the
 * browser's ordered language list, then `en-GB`.
 *
 * Auto-detected locales are not persisted, so changing the browser language
 * still takes effect on the next visit.
 */
export function resolveInitialLocale(): SupportedLocale {
  const stored = readStoredLocale()

  if (stored) {
    return stored
  }

  if (typeof navigator === 'undefined') {
    return DEFAULT_LOCALE
  }

  const languages = [...navigator.languages, navigator.language].filter(Boolean)

  return detectLocale(languages)
}
