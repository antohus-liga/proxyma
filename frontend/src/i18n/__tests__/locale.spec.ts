import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  applyDocumentLocale,
  DEFAULT_LOCALE,
  detectLocale,
  isSupportedLocale,
  LOCALE_STORAGE_KEY,
  matchLocale,
  persistLocale,
  readStoredLocale,
  resolveInitialLocale,
} from '../locale'

const originalLanguages = window.navigator.languages
const originalLanguage = window.navigator.language

function stubBrowserLanguages(languages: string[], language = languages[0] ?? 'en-US') {
  Object.defineProperty(window.navigator, 'languages', {
    configurable: true,
    get: () => languages,
  })
  Object.defineProperty(window.navigator, 'language', {
    configurable: true,
    get: () => language,
  })
}

beforeEach(() => {
  localStorage.clear()
})

afterEach(() => {
  Object.defineProperty(window.navigator, 'languages', {
    configurable: true,
    value: originalLanguages,
  })
  Object.defineProperty(window.navigator, 'language', {
    configurable: true,
    value: originalLanguage,
  })
  document.documentElement.lang = 'en-GB'
})

describe('matchLocale', () => {
  it('matches supported locales case-insensitively', () => {
    expect(matchLocale('pt-pt')).toBe('pt-PT')
    expect(matchLocale('EN-gb')).toBe('en-GB')
  })

  it('maps base languages to the shipped catalogue', () => {
    expect(matchLocale('en-US')).toBe('en-GB')
    expect(matchLocale('pt-BR')).toBe('pt-PT')
  })

  it('ignores unsupported and invalid tags', () => {
    expect(matchLocale('fr-FR')).toBeNull()
    expect(matchLocale('not a locale')).toBeNull()
  })
})

describe('detectLocale', () => {
  it('uses the first supported language in browser order', () => {
    expect(detectLocale(['fr-FR', 'pt-PT', 'en-GB'])).toBe('pt-PT')
    expect(detectLocale(['de-DE', 'en-US'])).toBe('en-GB')
  })

  it('falls back to en-GB when nothing matches', () => {
    expect(detectLocale([])).toBe(DEFAULT_LOCALE)
    expect(detectLocale(['fr-FR', 'de-DE'])).toBe(DEFAULT_LOCALE)
  })
})

describe('isSupportedLocale', () => {
  it('accepts only registered catalogues', () => {
    expect(isSupportedLocale('en-GB')).toBe(true)
    expect(isSupportedLocale('pt-PT')).toBe(true)
    expect(isSupportedLocale('pt-BR')).toBe(false)
    expect(isSupportedLocale(null)).toBe(false)
  })
})

describe('stored locale', () => {
  it('reads a supported stored value', () => {
    localStorage.setItem(LOCALE_STORAGE_KEY, 'pt-PT')

    expect(readStoredLocale()).toBe('pt-PT')
  })

  it('ignores an unsupported stored value', () => {
    localStorage.setItem(LOCALE_STORAGE_KEY, 'fr-FR')

    expect(readStoredLocale()).toBeNull()
  })

  it('persists an explicit choice', () => {
    persistLocale('pt-PT')

    expect(localStorage.getItem(LOCALE_STORAGE_KEY)).toBe('pt-PT')
  })
})

describe('resolveInitialLocale', () => {
  it('prefers an explicit stored choice over the browser', () => {
    localStorage.setItem(LOCALE_STORAGE_KEY, 'en-GB')
    stubBrowserLanguages(['pt-PT'])

    expect(resolveInitialLocale()).toBe('en-GB')
  })

  it('uses the browser when no choice is stored', () => {
    stubBrowserLanguages(['fr-FR', 'pt-BR'])

    expect(resolveInitialLocale()).toBe('pt-PT')
  })

  it('falls back to en-GB for unsupported browsers', () => {
    stubBrowserLanguages(['fr-FR', 'de-DE'])

    expect(resolveInitialLocale()).toBe('en-GB')
  })

  it('ignores an invalid stored value', () => {
    localStorage.setItem(LOCALE_STORAGE_KEY, 'fr-FR')
    stubBrowserLanguages(['pt-PT'])

    expect(resolveInitialLocale()).toBe('pt-PT')
  })
})

describe('applyDocumentLocale', () => {
  it('sets the language of the document', () => {
    applyDocumentLocale('pt-PT')

    expect(document.documentElement.lang).toBe('pt-PT')
  })
})
