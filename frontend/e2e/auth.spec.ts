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
    instance: '/api/v1/auth',
    code,
    ...extra,
  }
}

async function mockAnonymousRefresh(page: Page) {
  await page.route('**/api/v1/auth/refresh', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/problem+json',
      body: JSON.stringify(problem('INVALID_REFRESH_TOKEN', 401)),
    }),
  )
}

async function mockSignIn(page: Page) {
  await mockAnonymousRefresh(page)
  await page.route('**/api/v1/auth/login', (route) =>
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(loginResponse),
    }),
  )
  await page.route('**/api/v1/auth/logout', (route) => route.fulfill({ status: 204, body: '' }))
}

test.describe('sign in', () => {
  test('signs in and signs out', async ({ page }) => {
    await mockSignIn(page)
    await page.goto('/sign-in')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Sign in to Proxyma')
    await page.getByLabel('Email').fill('ana@example.com')
    await page.getByLabel(/^Password/).fill('correct horse battery staple')
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page).toHaveURL(/\/app$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Your account')
    await expect(page.getByRole('main').getByText('Ana', { exact: true })).toBeVisible()
    await expect(page.getByRole('main').getByText('ana@example.com')).toBeVisible()
    await expect(
      page.getByText("Organisation and proxy features aren't available yet."),
    ).toBeVisible()
    await expect(
      page.getByRole('navigation', { name: 'Account' }).getByRole('link', { name: 'Account' }),
    ).toBeVisible()
    await expect(
      page.getByRole('navigation', { name: 'Platform administration' }),
    ).toHaveCount(0)

    await page.getByRole('button', { name: 'Sign out' }).click()

    await expect(page).toHaveURL(/\/sign-in$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Sign in to Proxyma')
  })

  test('shows the invalid credentials message and keeps the email', async ({ page }) => {
    await mockAnonymousRefresh(page)
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill({
        status: 401,
        contentType: 'application/problem+json',
        body: JSON.stringify(problem('INVALID_CREDENTIALS', 401)),
      }),
    )

    await page.goto('/sign-in')
    await page.getByLabel('Email').fill('ana@example.com')
    await page.getByLabel(/^Password/).fill('wrong password')
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page.getByText('Email or password is incorrect.')).toBeVisible()
    await expect(page.getByLabel('Email')).toHaveValue('ana@example.com')
    await expect(page.getByLabel(/^Password/)).toHaveValue('')
  })

  test('disables submission while rate limited', async ({ page }) => {
    await mockAnonymousRefresh(page)
    await page.route('**/api/v1/auth/login', (route) =>
      route.fulfill({
        status: 429,
        headers: { 'retry-after': '30' },
        contentType: 'application/problem+json',
        body: JSON.stringify(problem('RATE_LIMITED', 429)),
      }),
    )

    await page.goto('/sign-in')
    await page.getByLabel('Email').fill('ana@example.com')
    await page.getByLabel(/^Password/).fill('correct horse battery staple')
    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page.getByText(/Too many attempts\. Try again in \d+ seconds\./)).toBeVisible()
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeDisabled()
  })

  test('validates required fields before submitting', async ({ page }) => {
    await mockAnonymousRefresh(page)
    await page.goto('/sign-in')

    await page.getByRole('button', { name: 'Sign in' }).click()

    await expect(page.getByText('Enter your email address.')).toBeVisible()
    await expect(page.getByText('Enter your password.')).toBeVisible()
  })
})

test.describe('create account', () => {
  test('creates an account and links to sign in with the email prefilled', async ({ page }) => {
    await mockAnonymousRefresh(page)
    await page.route('**/api/v1/auth/register', (route) =>
      route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify(user),
      }),
    )

    await page.goto('/create-account')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      'Create your Proxyma account',
    )
    await page.getByLabel('Display name').fill('Ana')
    await page.getByLabel('Email').fill('ana@example.com')
    await page.getByLabel(/^Password/).fill('correct horse battery staple')
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Account created')
    await expect(page.getByText('Sign in to continue.')).toBeVisible()

    await page.getByRole('link', { name: 'Sign in' }).click()

    await expect(page).toHaveURL(/\/sign-in\?email=ana%40example\.com$/)
    await expect(page.getByLabel('Email')).toHaveValue('ana@example.com')
  })

  test('links an existing email to sign in', async ({ page }) => {
    await mockAnonymousRefresh(page)
    await page.route('**/api/v1/auth/register', (route) =>
      route.fulfill({
        status: 409,
        contentType: 'application/problem+json',
        body: JSON.stringify(problem('EMAIL_TAKEN', 409)),
      }),
    )

    await page.goto('/create-account')
    await page.getByLabel('Display name').fill('Ana')
    await page.getByLabel('Email').fill('ana@example.com')
    await page.getByLabel(/^Password/).fill('correct horse battery staple')
    await page.getByRole('button', { name: 'Create account' }).click()

    const link = page.getByRole('link', { name: 'Sign in instead' })
    await expect(link).toBeVisible()
    await expect(page.getByText('An account with this email already exists.')).toBeVisible()

    await link.click()

    await expect(page).toHaveURL(/\/sign-in\?email=ana%40example\.com$/)
    await expect(page.getByLabel('Email')).toHaveValue('ana@example.com')
  })

  test('validates password length before submitting', async ({ page }) => {
    await mockAnonymousRefresh(page)
    await page.goto('/create-account')

    await page.getByLabel('Display name').fill('Ana')
    await page.getByLabel('Email').fill('ana@example.com')
    await page.getByLabel(/^Password/).fill('short')
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page.getByText('Use at least 10 characters.', { exact: true })).toBeVisible()
  })
})

for (const path of ['/sign-in', '/create-account']) {
  for (const theme of ['light', 'dark']) {
    test(`${path} has no accessibility violations in ${theme} mode`, async ({ page }) => {
      await mockAnonymousRefresh(page)
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
    await mockAnonymousRefresh(page)
    await page.setViewportSize({ width: 320, height: 900 })
    await page.goto(path)

    const widths = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth,
    }))

    expect(widths.scrollWidth).toBeLessThanOrEqual(widths.clientWidth)
  })
}
