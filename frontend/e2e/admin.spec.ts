import { test, expect, type Page } from '@playwright/test'

const admin = {
  id: 'admin-1',
  email: 'admin@example.com',
  display_name: 'Platform Admin',
  is_platform_admin: true,
}

const member = {
  id: 'user-1',
  email: 'ana@example.com',
  display_name: 'Ana',
  is_platform_admin: false,
}

const adminSession = {
  access_token: 'admin-token',
  token_type: 'Bearer',
  expires_in: 300,
  user: admin,
}

const stats = {
  users: { total: 42, suspended: 1 },
  orgs: { total: 31, online: 28, offline: 3 },
}

const userSummary = {
  id: 'user-1',
  email: 'ana@example.com',
  display_name: 'Ana',
  status: 'active',
  created_at: '2026-09-01T10:00:00Z',
  last_login_at: '2026-10-18T09:12:00Z',
  owns_org: true,
  membership_count: 2,
}

const userDetail = {
  id: 'user-1',
  email: 'ana@example.com',
  display_name: 'Ana',
  status: 'active',
  suspension: null,
  is_platform_admin: false,
  created_at: '2026-09-01T10:00:00Z',
  last_login_at: '2026-10-18T09:12:00Z',
  owned_org: { id: 'org-1', name: 'Acme' },
  memberships: [
    {
      org_id: 'org-1',
      org_name: 'Acme',
      role: 'owner',
      joined_at: '2026-09-01T10:05:00Z',
    },
  ],
  active_sessions: 1,
}

const suspendedUserDetail = {
  ...userDetail,
  status: 'suspended',
  suspension: {
    reason: 'Repeated abuse reports',
    suspended_at: '2026-10-18T14:35:12Z',
    suspended_by: { user_id: 'admin-1', display_name: 'Platform Admin' },
  },
  active_sessions: 0,
}

const orgSummary = {
  id: 'org-1',
  name: 'Acme',
  state: 'ONLINE',
  owner: { user_id: 'user-1', display_name: 'Ana' },
  member_count: 3,
  created_at: '2026-09-01T10:00:00Z',
}

const orgDetail = {
  id: 'org-1',
  name: 'Acme',
  state: 'ONLINE',
  created_at: '2026-09-01T10:00:00Z',
  owner: { user_id: 'user-1', display_name: 'Ana' },
  members: [
    { user_id: 'user-1', display_name: 'Ana', role: 'owner' },
    { user_id: 'user-2', display_name: 'Bo', role: 'member' },
  ],
  proxy: {
    last_heartbeat_at: '2026-10-18T14:35:12Z',
    running_config_version: 7,
    latest_config_version: 7,
    config_status: 'applied',
    active_ban_count: 4,
  },
}

const auditEntry = {
  id: 'audit-1',
  timestamp: '2026-10-18T14:35:12Z',
  admin: { user_id: 'admin-1', display_name: 'Platform Admin' },
  action: 'user.view',
  target: { type: 'user', id: 'user-1' },
  summary: 'Viewed ana@example.com',
  source_ip: '203.0.113.10',
}

function json(body: unknown, status = 200) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  }
}

function problem(code: string, status = 400) {
  return {
    status,
    contentType: 'application/problem+json',
    body: JSON.stringify({
      type: 'about:blank',
      title: 'Request failed',
      status,
      detail: 'Request failed',
      instance: '/api/v1/admin',
      code,
    }),
  }
}

async function mockSession(page: Page, user = admin) {
  await page.route('**/api/v1/auth/refresh', (route) =>
    route.fulfill(json({ ...adminSession, user })),
  )
}

async function mockOverview(page: Page) {
  await mockSession(page)
  await page.route('**/api/v1/admin/stats', (route) => route.fulfill(json(stats)))
}

test.describe('platform administration', () => {
  test('shows platform counts and separates admin navigation from the account', async ({
    page,
  }) => {
    await mockOverview(page)
    await page.goto('/admin')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Platform overview')
    await expect(page.getByText('42')).toBeVisible()
    await expect(page.getByText('31')).toBeVisible()

    const navigation = page.getByRole('navigation', { name: 'Platform administration' })
    await expect(navigation.getByRole('link', { name: 'Overview' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    await expect(navigation.getByRole('link', { name: 'Users' })).toBeVisible()
    await expect(
      page.locator('.app-sidebar').getByRole('link', { name: 'Settings' }),
    ).toBeVisible()
    await expect(page.getByRole('link', { name: 'Back to settings' })).toHaveCount(0)
    await expect(page.getByText('Platform administration').first()).toBeVisible()
  })

  test('opens the mobile drawer with admin navigation available', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await mockOverview(page)
    await page.goto('/admin')

    const usersLink = page
      .getByRole('navigation', { name: 'Platform administration' })
      .getByRole('link', { name: 'Users' })
    await expect(usersLink).toBeHidden()

    await page.getByRole('button', { name: 'Open navigation' }).click()

    await expect(usersLink).toBeVisible()
    await expect(page.getByRole('button', { name: 'Sign out' })).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(usersLink).toBeHidden()
  })

  test('lists users and suspends an account with step-up', async ({ page }) => {
    await mockSession(page)
    await page.route('**/api/v1/admin/users', (route) => {
      if (route.request().method() === 'GET') {
        return route.fulfill(json({ items: [userSummary], next_cursor: null }))
      }

      return route.fulfill(problem('NOT_FOUND', 404))
    })
    await page.route('**/api/v1/admin/users/user-1', (route) => route.fulfill(json(userDetail)))
    await page.route('**/api/v1/admin/users/user-1/suspend', (route) => {
      expect(route.request().method()).toBe('POST')
      expect(route.request().postDataJSON()).toEqual({
        reason: 'Repeated abuse reports',
        password: 'admin-password',
      })
      return route.fulfill(json(suspendedUserDetail))
    })

    await page.goto('/admin/users')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Users')
    await expect(page.getByRole('link', { name: 'Ana' })).toBeVisible()

    await page.getByRole('link', { name: 'Ana' }).click()
    await expect(page).toHaveURL(/\/admin\/users\/user-1$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Ana')
    await expect(page.getByText('Acme').first()).toBeVisible()

    await page.getByRole('button', { name: 'Suspend account' }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog.getByRole('heading', { name: 'Suspend Ana?' })).toBeVisible()
    await dialog.getByRole('button', { name: 'Suspend account' }).click()
    await expect(dialog.getByText('Enter a reason.')).toBeVisible()
    await expect(dialog.getByText('Enter your password.')).toBeVisible()

    await dialog.getByLabel('Reason').fill('Repeated abuse reports')
    await dialog.getByLabel('Your password').fill('admin-password')
    await dialog.getByRole('button', { name: 'Suspend account' }).click()

    await expect(page.getByText('The account has been suspended.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Unsuspend account' })).toBeVisible()
    await expect(page.getByText('Repeated abuse reports')).toBeVisible()
  })

  test('keeps organisation detail read-only and links the owner to user details', async ({
    page,
  }) => {
    await mockSession(page)
    await page.route('**/api/v1/admin/orgs', (route) =>
      route.fulfill(json({ items: [orgSummary], next_cursor: null })),
    )
    await page.route('**/api/v1/admin/orgs/org-1', (route) => route.fulfill(json(orgDetail)))

    await page.goto('/admin/orgs')
    await page.getByRole('link', { name: 'Acme' }).first().click()

    await expect(page).toHaveURL(/\/admin\/orgs\/org-1$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Acme')
    await expect(page.getByText('This page is read-only.')).toBeVisible()
    await expect(page.getByText('Applied').first()).toBeVisible()
    await expect(page.getByRole('link', { name: 'Open owner account' })).toHaveAttribute(
      'href',
      '/admin/users/user-1',
    )
    await expect(page.getByRole('button', { name: /suspend/i })).toHaveCount(0)
  })

  test('filters audit by range and exposes the raw action code in details', async ({ page }) => {
    await mockSession(page)
    await page.route('**/api/v1/admin/audit**', (route) =>
      route.fulfill(json({ items: [auditEntry], next_cursor: null })),
    )

    await page.goto('/admin/audit')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Audit')
    await expect(page.getByRole('cell', { name: 'Viewed user' })).toBeVisible()

    await page.getByRole('button', { name: 'Details' }).click()
    const dialog = page.getByRole('dialog')
    await expect(dialog.getByRole('heading', { name: 'Audit event' })).toBeVisible()
    await expect(dialog.getByText('user.view')).toBeVisible()
    await expect(dialog.getByText('203.0.113.10')).toBeVisible()

    await page.keyboard.press('Escape')
    await expect(dialog).toBeHidden()

    await page.getByLabel('From').fill('2026-10-02T10:00')
    await page.getByLabel('To (exclusive)').fill('2026-10-01T10:00')
    await page.getByRole('button', { name: 'Apply filters' }).click()

    await expect(page.getByText('The end of the range must be after the start.')).toBeVisible()
  })

  test('shows the normal not-found page to a non-admin', async ({ page }) => {
    await mockSession(page, member)
    await page.goto('/admin')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Page not found')
    await expect(page.getByText('Platform overview')).toHaveCount(0)
  })

  test('shows the normal not-found page when the admin API is disabled', async ({ page }) => {
    await mockSession(page)
    await page.route('**/api/v1/admin/stats', (route) => route.fulfill(problem('NOT_FOUND', 404)))

    await page.goto('/admin')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Page not found')
    await expect(page.getByText('Platform overview')).toHaveCount(0)
    await expect(page.getByText('42')).toHaveCount(0)
  })
})
