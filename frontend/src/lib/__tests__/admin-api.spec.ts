import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { adminApi } from '../admin-api'

const fetchMock = vi.fn<typeof fetch>()

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

describe('adminApi', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('encodes user filters into the query string', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ items: [], next_cursor: null }))

    await adminApi.listUsers({
      q: 'ana smith',
      status: 'suspended',
      limit: 50,
      cursor: 'cursor-1',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/users?q=ana+smith&status=suspended&limit=50&cursor=cursor-1',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('omits empty filters and sends the risky action body unchanged', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        id: 'user-1',
        email: 'ana@example.com',
        display_name: 'Ana',
        status: 'suspended',
        suspension: null,
        is_platform_admin: false,
        created_at: '2026-01-01T00:00:00Z',
        last_login_at: null,
        owned_org: null,
        memberships: [],
        active_sessions: 0,
      }),
    )

    await adminApi.suspendUser('user/1', {
      reason: 'Repeated abuse reports',
      password: 'secret',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/users/user%2F1/suspend',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          reason: 'Repeated abuse reports',
          password: 'secret',
        }),
      }),
    )
  })

  it('passes audit range and exact action filters', async () => {
    fetchMock.mockResolvedValue(jsonResponse({ items: [], next_cursor: null }))

    await adminApi.listAudit({
      from: '2026-10-01T00:00:00.000Z',
      to: '2026-10-02T00:00:00.000Z',
      action: 'user.view',
    })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/admin/audit?from=2026-10-01T00%3A00%3A00.000Z&to=2026-10-02T00%3A00%3A00.000Z&action=user.view',
      expect.objectContaining({ method: 'GET' }),
    )
  })

  it('turns a problem response into a typed error', async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          type: 'about:blank',
          title: 'Not found',
          status: 404,
          detail: 'Not found',
          instance: '/api/v1/admin/stats',
          code: 'NOT_FOUND',
        }),
        {
          status: 404,
          headers: { 'content-type': 'application/problem+json' },
        },
      ),
    )

    await expect(adminApi.stats()).rejects.toMatchObject({
      code: 'NOT_FOUND',
      status: 404,
    })
  })
})
