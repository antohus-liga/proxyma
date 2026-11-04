import { test, expect, type Page } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

const user = {
  id: 'user-1',
  email: 'ana@example.com',
  display_name: 'Ana',
  is_platform_admin: false,
}

const loginResponse = {
  access_token: 'access-token',
  token_type: 'Bearer',
  expires_in: 900,
  user,
}

function problem(code: string, status = 422, extra: Record<string, unknown> = {}) {
  return {
    type: 'about:blank',
    title: 'Request failed',
    status,
    detail: 'Request failed',
    instance: '/api/v1/users/me',
    code,
    ...extra,
  }
}

function json(body: unknown, status = 200) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  }
}

async function mockSession(page: Page) {
  await page.route('**/api/v1/auth/refresh', (route) =>
    route.fulfill(json(loginResponse)),
  )
  await page.route('**/api/v1/auth/logout', (route) => route.fulfill({ status: 204, body: '' }))
}

test.describe('account profile', () => {
  test('updates the display name', async ({ page }) => {
    await mockSession(page)

    let patchBody: unknown
    await page.route('**/api/v1/users/me', async (route) => {
      patchBody = route.request().postDataJSON()
      await route.fulfill(json({ ...user, display_name: 'Ana Smith' }))
    })

    await page.goto('/app')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Your account')
    await expect(page.getByRole('main').getByText('ana@example.com')).toBeVisible()
    await expect(page.getByText('Standard account')).toBeVisible()

    await page.getByLabel('Display name').fill('Ana Smith')
    await page.getByRole('button', { name: 'Save changes' }).click()

    await expect(page.getByText('Your profile has been updated.')).toBeVisible()
    await expect(page.getByRole('banner').getByText('Ana Smith')).toBeVisible()
    expect(patchBody).toEqual({ display_name: 'Ana Smith' })
  })

  test('validates an empty display name', async ({ page }) => {
    await mockSession(page)
    await page.goto('/app')

    await page.getByLabel('Display name').fill('   ')
    await page.getByRole('button', { name: 'Save changes' }).click()

    await expect(page.getByText('Enter a display name.')).toBeVisible()
  })
})

test.describe('account password', () => {
  test('changes the password and returns to sign in', async ({ page }) => {
    await mockSession(page)

    let passwordBody: unknown
    await page.route('**/api/v1/users/me/password', async (route) => {
      passwordBody = route.request().postDataJSON()
      await route.fulfill({ status: 204, body: '' })
    })

    await page.goto('/app/password')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Your account')
    await expect(page.getByText('You will be signed out')).toBeVisible()
    await expect(
      page.getByRole('navigation', { name: 'Account settings' }).getByRole('link', {
        name: 'Password',
      }),
    ).toHaveAttribute('aria-current', 'page')

    await page.getByLabel(/^Current password/).fill('old-password')
    await page.getByLabel(/^New password/).fill('new-password-123')
    await page.getByLabel(/^Confirm new password/).fill('new-password-123')
    await page.getByRole('button', { name: 'Change password' }).click()

    await expect(page).toHaveURL(/\/sign-in\?reason=password-changed/)
    await expect(page).toHaveURL(/email=ana@example\.com/)
    await expect(page.getByText('Password changed. Sign in with your new password.')).toBeVisible()
    await expect(page.getByLabel('Email')).toHaveValue('ana@example.com')
    expect(passwordBody).toEqual({
      current_password: 'old-password',
      new_password: 'new-password-123',
    })
  })

  test('shows a rejected current password and clears it', async ({ page }) => {
    await mockSession(page)
    await page.route('**/api/v1/users/me/password', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/problem+json',
        body: JSON.stringify(problem('REAUTH_FAILED', 401)),
      }),
    )

    await page.goto('/app/password')
    await page.getByLabel(/^Current password/).fill('wrong-password')
    await page.getByLabel(/^New password/).fill('new-password-123')
    await page.getByLabel(/^Confirm new password/).fill('new-password-123')
    await page.getByRole('button', { name: 'Change password' }).click()

    await expect(page.getByText('That password is incorrect. Try again.')).toBeVisible()
    await expect(page.getByLabel(/^Current password/)).toHaveValue('')
  })

  test('rejects a mismatched confirmation before submitting', async ({ page }) => {
    await mockSession(page)
    await page.goto('/app/password')

    await page.getByLabel(/^Current password/).fill('old-password')
    await page.getByLabel(/^New password/).fill('new-password-123')
    await page.getByLabel(/^Confirm new password/).fill('different-password')
    await page.getByRole('button', { name: 'Change password' }).click()

    await expect(page.getByText('The passwords do not match.')).toBeVisible()
    await expect(page).toHaveURL(/\/app\/password$/)
  })

  test('shows a weak-password error on the new-password field', async ({ page }) => {
    await mockSession(page)
    await page.route('**/api/v1/users/me/password', (route) =>
      route.fulfill({
        status: 422,
        contentType: 'application/problem+json',
        body: JSON.stringify(problem('WEAK_PASSWORD', 422)),
      }),
    )

    await page.goto('/app/password')
    await page.getByLabel(/^Current password/).fill('old-password')
    await page.getByLabel(/^New password/).fill('new-password-123')
    await page.getByLabel(/^Confirm new password/).fill('new-password-123')
    await page.getByRole('button', { name: 'Change password' }).click()

    await expect(page.getByText(/Choose a stronger password\./)).toBeVisible()
  })
})

for (const path of ['/app', '/app/password']) {
  for (const theme of ['light', 'dark']) {
    test(`${path} has no accessibility violations in ${theme} mode`, async ({ page }) => {
      await mockSession(page)
      await page.addInitScript((value) => localStorage.setItem('proxyma.theme', value), theme)
      await page.goto(path)
      await expect(page.getByRole('heading', { level: 1 })).toBeVisible()

      const results = await new AxeBuilder({ page })
        .exclude('.vue-devtools__anchor-btn')
        .analyze()

      expect(results.violations).toEqual([])
    })
  }

  test(`${path} does not overflow at 320px`, async ({ page }) => {
    await mockSession(page)
    await page.setViewportSize({ width: 320, height: 900 })
    await page.goto(path)

    const widths = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }))

    expect(widths.scrollWidth).toBeLessThanOrEqual(widths.clientWidth)
  })
}
