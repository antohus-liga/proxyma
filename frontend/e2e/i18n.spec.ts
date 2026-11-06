import { test, expect } from '@playwright/test'
import AxeBuilder from '@axe-core/playwright'

test.describe('Portuguese locale', () => {
  test.use({ locale: 'pt-PT' })

  test('detects pt-PT from the browser', async ({ page }) => {
    await page.goto('/')

    await expect(page.locator('html')).toHaveAttribute('lang', 'pt-PT')
    await expect(page).toHaveTitle('Proxyma — Sempre perto, sempre presente.')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText(
      'Sempre perto, sempre presente.',
    )
  })

  test('persists an explicit language choice', async ({ page }) => {
    await page.goto('/')

    await page.getByLabel('Idioma').selectOption('en-GB')

    await expect(page.locator('html')).toHaveAttribute('lang', 'en-GB')
    await expect(page).toHaveTitle('Proxyma — Always near, always here.')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Always near, always here.')

    await page.reload()

    await expect(page.locator('html')).toHaveAttribute('lang', 'en-GB')
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Always near, always here.')
  })

  test('has no detected accessibility violations', async ({ page }) => {
    await page.goto('/')

    const results = await new AxeBuilder({ page }).exclude('.vue-devtools__anchor-btn').analyze()

    expect(results.violations).toEqual([])
  })
})
