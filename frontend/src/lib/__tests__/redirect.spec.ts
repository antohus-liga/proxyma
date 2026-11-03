import { describe, expect, it } from 'vitest'
import { buildAuthPath, safeRedirect } from '@/lib/redirect'

describe('safeRedirect', () => {
  it('accepts same-origin paths with queries', () => {
    expect(safeRedirect('/app/projects?page=2')).toBe('/app/projects?page=2')
  })

  it('rejects protocol-relative and absolute URLs', () => {
    expect(safeRedirect('//evil.example/app')).toBeUndefined()
    expect(safeRedirect('https://evil.example/app')).toBeUndefined()
    expect(safeRedirect('javascript:alert(1)')).toBeUndefined()
  })

  it('rejects backslashes and control characters', () => {
    expect(safeRedirect('/\\evil.example')).toBeUndefined()
    expect(safeRedirect('/app\u0000')).toBeUndefined()
  })

  it('rejects the auth routes to avoid redirect loops', () => {
    expect(safeRedirect('/sign-in')).toBeUndefined()
    expect(safeRedirect('/create-account?redirect=%2Fapp')).toBeUndefined()
  })

  it('ignores non-string values', () => {
    expect(safeRedirect(undefined)).toBeUndefined()
    expect(safeRedirect(['/app'])).toBeUndefined()
  })
})

describe('buildAuthPath', () => {
  it('omits empty values and encodes query parameters', () => {
    expect(
      buildAuthPath('/sign-in', {
        email: 'ana@example.com',
        redirect: '/app?page=2',
        reason: undefined,
      }),
    ).toBe('/sign-in?email=ana%40example.com&redirect=%2Fapp%3Fpage%3D2')
  })

  it('returns the bare path when there is no query', () => {
    expect(buildAuthPath('/create-account')).toBe('/create-account')
  })
})
