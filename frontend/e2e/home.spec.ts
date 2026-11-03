import { test, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

test('serves the public homepage', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveTitle('Proxyma — Always near, always here.')
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('Always near, always here.')
  await expect(page.getByRole('link', { name: 'Create an account' }).first()).toBeVisible()
  await expect(page.getByRole('link', { name: 'Sign in' }).first()).toBeVisible()
  await expect(page.locator('picture img')).toBeVisible()
})

test('homepage calls to action reach the auth pages', async ({ page }) => {
  await page.route('**/api/v1/auth/refresh', (route) =>
    route.fulfill({
      status: 401,
      contentType: 'application/problem+json',
      body: JSON.stringify({
        type: 'about:blank',
        title: 'Unauthorised',
        status: 401,
        detail: 'No refresh token',
        instance: '/api/v1/auth/refresh',
        code: 'INVALID_REFRESH_TOKEN',
      }),
    }),
  )

  await page.goto('/')
  await page.getByRole('link', { name: 'Create an account' }).first().click()

  await expect(page).toHaveURL(/\/create-account$/)
  await expect(page.getByRole('heading', { level: 1 })).toHaveText(
    'Create your Proxyma account',
  )
})

test('homepage has no detected accessibility violations in light mode', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('proxyma.theme', 'light'))
  await page.goto('/')

  const results = await new AxeBuilder({ page })
    .exclude('.vue-devtools__anchor-btn')
    .analyze()

  expect(results.violations).toEqual([])
})

test('homepage has no detected accessibility violations in dark mode', async ({ page }) => {
  await page.addInitScript(() => localStorage.setItem('proxyma.theme', 'dark'))
  await page.goto('/')

  const results = await new AxeBuilder({ page })
    .exclude('.vue-devtools__anchor-btn')
    .analyze()

  expect(results.violations).toEqual([])
})

test('homepage does not overflow at 320px', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 900 })
  await page.goto('/')

  const widths = await page.evaluate(() => ({
    scrollWidth: document.documentElement.scrollWidth,
    clientWidth: document.documentElement.clientWidth,
  }))

  expect(widths.scrollWidth).toBeLessThanOrEqual(widths.clientWidth)
  await expect(page.locator('picture')).toBeHidden()
})
