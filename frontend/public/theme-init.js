(() => {
  let stored = null
  try {
    stored = localStorage.getItem('proxyma.theme')
  } catch {
    // Storage can be unavailable in private mode; follow the system preference.
  }

  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  const theme = stored === 'light' || stored === 'dark' ? stored : prefersDark ? 'dark' : 'light'

  document.documentElement.dataset.theme = theme
  document.documentElement.style.setProperty('color-scheme', theme)
})()
