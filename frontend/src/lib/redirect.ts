const AUTH_PATHS = new Set(['/sign-in', '/create-account'])

/**
 * Accepts only same-origin, in-app paths. Rejects protocol-relative URLs,
 * backslashes and the auth routes themselves to avoid redirect loops.
 */
export function safeRedirect(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined
  }

  const path = value.trim()

  if (!path.startsWith('/') || path.startsWith('//') || path.includes('\\')) {
    return undefined
  }

  for (const character of path) {
    const code = character.codePointAt(0)

    if (code !== undefined && (code <= 31 || code === 127)) {
      return undefined
    }
  }

  const pathname = path.split(/[?#]/, 1)[0] ?? ''

  if (AUTH_PATHS.has(pathname)) {
    return undefined
  }

  return path
}

export function buildAuthPath(
  pathname: '/sign-in' | '/create-account',
  query: Record<string, string | undefined> = {},
): string {
  const params = new URLSearchParams()

  for (const [key, value] of Object.entries(query)) {
    if (value) {
      params.set(key, value)
    }
  }

  const search = params.toString()
  return search ? `${pathname}?${search}` : pathname
}
