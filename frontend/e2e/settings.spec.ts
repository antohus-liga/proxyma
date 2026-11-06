import { test, expect, type Page } from '@playwright/test'

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

function json(body: unknown, status = 200) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify(body),
  }
}

async function mockSession(page: Page) {
  await page.route('**/api/v1/auth/refresh', (route) => route.fulfill(json(loginResponse)))
  await page.route('**/api/v1/auth/logout', (route) => route.fulfill({ status: 204, body: '' }))
}

test.describe('settings', () => {
  test('redirects the settings root and legacy account routes', async ({ page }) => {
    await mockSession(page)

    await page.goto('/settings')
    await expect(page).toHaveURL(/\/settings\/account$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Settings')

    await page.goto('/app')
    await expect(page).toHaveURL(/\/settings\/account$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Settings')

    await page.goto('/app/password')
    await expect(page).toHaveURL(/\/settings\/security$/)
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Settings')
  })

  test('switches theme and language from General and keeps them', async ({ page }) => {
    await mockSession(page)
    await page.goto('/settings/general')

    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Settings')
    await expect(page.getByRole('heading', { level: 2, name: 'Appearance' })).toBeVisible()
    await expect(page.getByRole('heading', { level: 2, name: 'Language' })).toBeVisible()

    await page.getByRole('button', { name: 'Use dark theme' }).click()
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    expect(await page.evaluate(() => localStorage.getItem('proxyma.theme'))).toBe('dark')

    await page.getByRole('combobox', { name: 'Language' }).selectOption('pt-PT')
    await expect(page.locator('html')).toHaveAttribute('lang', 'pt-PT')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Definições')
    await expect(page.getByRole('heading', { level: 2, name: 'Idioma' })).toBeVisible()
    expect(await page.evaluate(() => localStorage.getItem('proxyma.locale'))).toBe('pt-PT')

    await page.reload()

    await expect(page.locator('html')).toHaveAttribute('lang', 'pt-PT')
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Definições')
  })

  test('pins Settings above Sign out in the sidebar footer', async ({ page }) => {
    await mockSession(page)
    await page.setViewportSize({ width: 1280, height: 800 })
    await page.goto('/settings/account')

    const sidebar = page.locator('.app-sidebar')
    const settingsLink = sidebar.getByRole('link', { name: 'Settings' })
    const signOut = sidebar.getByRole('button', { name: 'Sign out' })

    const sidebarBox = await sidebar.boundingBox()
    const linkBox = await settingsLink.boundingBox()
    const signOutBox = await signOut.boundingBox()

    expect(sidebarBox).not.toBeNull()
    expect(linkBox).not.toBeNull()
    expect(signOutBox).not.toBeNull()

    expect(linkBox!.y).toBeGreaterThan(sidebarBox!.y + sidebarBox!.height / 2)
    expect(linkBox!.y + linkBox!.height).toBeLessThanOrEqual(signOutBox!.y)
  })

  test('navigates between settings sections', async ({ page }) => {
    await mockSession(page)
    await page.goto('/settings/general')

    const sections = page.getByRole('navigation', { name: 'Settings sections' })

    await sections.getByRole('link', { name: 'Security' }).click()
    await expect(page).toHaveURL(/\/settings\/security$/)
    await expect(sections.getByRole('link', { name: 'Security' })).toHaveAttribute(
      'aria-current',
      'page',
    )

    await sections.getByRole('link', { name: 'Account' }).click()
    await expect(page).toHaveURL(/\/settings\/account$/)
    await expect(page.getByLabel('Display name')).toBeVisible()
  })
})
