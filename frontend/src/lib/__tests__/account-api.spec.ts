import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { accountApi } from '../account-api'

const fetchMock = vi.fn<typeof fetch>()

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json' },
  })
}

describe('accountApi', () => {
  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('patches the display name', async () => {
    const user = {
      id: 'user-1',
      email: 'ana@example.com',
      display_name: 'Ana Smith',
      is_platform_admin: false,
    }
    fetchMock.mockResolvedValue(jsonResponse(user))

    const result = await accountApi.updateProfile({ display_name: 'Ana Smith' })

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/users/me',
      expect.objectContaining({
        method: 'PATCH',
        body: JSON.stringify({ display_name: 'Ana Smith' }),
      }),
    )
    expect(result).toEqual(user)
  })

  it('posts the password change and accepts an empty 204', async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }))

    await expect(
      accountApi.changePassword({
        current_password: 'old-password',
        new_password: 'new-password-123',
      }),
    ).resolves.toBeUndefined()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/users/me/password',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          current_password: 'old-password',
          new_password: 'new-password-123',
        }),
      }),
    )
  })
})
