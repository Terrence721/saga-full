import { expect, test } from '@playwright/test'

test('order cart checkout completes', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: /Order online/i }).click()

  await page.getByRole('button', { name: /Add Diavola to order/i }).click()
  await expect(page.getByText(/1 × Diavola/)).toBeVisible()

  await page.getByPlaceholder('Your name').fill('Pat Tester')
  await page.getByRole('button', { name: /Place pickup order/i }).click()
  await expect(page.getByRole('heading', { name: /Order online\./i })).not.toBeVisible()
})

test('reservation form can be submitted', async ({ page }) => {
  await page.goto('/')
  await page.getByRole('button', { name: /Request a table/i }).click()

  await page.getByLabel('Name').fill('Pat Tester')
  await page.getByLabel('Email').fill('pat@example.com')

  await page.getByRole('button', { name: /Send table request/i }).click()
  await expect(page.getByRole('heading', { name: /We’ll be in touch\./i })).toBeVisible()
})
