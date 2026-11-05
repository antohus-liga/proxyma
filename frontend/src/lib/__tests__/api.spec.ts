import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, ApiError } from '../api'

const fetchMock = vi.fn<typeof fetch>()

function problemResponse(
  status: number,
  code: string,
  headers: Record<string, string> = {},
): Response {
  return new Response(
    JSON.stringify({
      type: 'about:blank',
      title: 'Request failed',
      status,
      detail: 'Request failed',
      instance: '/api/v1/auth/login',
      code,
    }),
    {
      status,
      headers: { 'content-type': 'application/problem+json', ...headers },
    },
  )
}

describe('api error retry metadata', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('does not treat RateLimit-Reset as a retry directive on validation errors', async () => {
    fetchMock.mockResolvedValue(
      problemResponse(422, 'WEAK_PASSWORD', { 'ratelimit-reset': '60' }),
    )

    await expect(
      api.login({ email: 'ana@example.com', password: 'password' }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      code: 'WEAK_PASSWORD',
      retryAfterSeconds: null,
    } satisfies Partial<ApiError>)
  })

  it('does not treat RateLimit-Reset as a retry directive on credential errors', async () => {
    fetchMock.mockResolvedValue(
      problemResponse(401, 'INVALID_CREDENTIALS', { 'ratelimit-reset': '60' }),
    )

    const error = await api
      .login({ email: 'ana@example.com', password: 'wrong password' })
      .catch((cause: unknown) => cause)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ code: 'INVALID_CREDENTIALS', retryAfterSeconds: null })
  })

  it('reads Retry-After from a 429 response', async () => {
    fetchMock.mockResolvedValue(
      problemResponse(429, 'RATE_LIMITED', {
        'retry-after': '30',
        'ratelimit-reset': '60',
      }),
    )

    await expect(
      api.login({ email: 'ana@example.com', password: 'password' }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      code: 'RATE_LIMITED',
      retryAfterSeconds: 30,
    } satisfies Partial<ApiError>)
  })

  it('ignores RateLimit-Reset when a 429 has no Retry-After', async () => {
    fetchMock.mockResolvedValue(
      problemResponse(429, 'RATE_LIMITED', { 'ratelimit-reset': '60' }),
    )

    await expect(
      api.login({ email: 'ana@example.com', password: 'password' }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      code: 'RATE_LIMITED',
      retryAfterSeconds: null,
    } satisfies Partial<ApiError>)
  })
})
