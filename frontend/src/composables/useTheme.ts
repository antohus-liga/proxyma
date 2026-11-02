import { computed, ref, watch } from 'vue'

export type ThemePreference = 'system' | 'light' | 'dark'
export type ResolvedTheme = 'light' | 'dark'

const STORAGE_KEY = 'proxyma.theme'

const mediaQuery =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-color-scheme: dark)')
    : null

function readStoredPreference(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') {
      return stored
    }
  } catch {
    // Storage can be unavailable in private mode; fall back to system.
  }

  return 'system'
}

const preference = ref<ThemePreference>(readStoredPreference())
const systemTheme = ref<ResolvedTheme>(mediaQuery?.matches ? 'dark' : 'light')
const resolvedTheme = computed<ResolvedTheme>(() =>
  preference.value === 'system' ? systemTheme.value : preference.value,
)

function applyTheme(theme: ResolvedTheme) {
  document.documentElement.dataset.theme = theme
  document.documentElement.style.setProperty('color-scheme', theme)
}

function persistPreference(value: ThemePreference) {
  try {
    localStorage.setItem(STORAGE_KEY, value)
  } catch {
    // Keep the preference in memory when storage is unavailable.
  }
}

mediaQuery?.addEventListener('change', (event) => {
  systemTheme.value = event.matches ? 'dark' : 'light'
})

watch(resolvedTheme, applyTheme, { immediate: true })

export function useTheme() {
  function setPreference(value: ThemePreference) {
    preference.value = value
    persistPreference(value)
  }

  return {
    preference,
    resolvedTheme,
    setPreference,
  }
}
