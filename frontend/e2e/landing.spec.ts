import { expect, test } from '@playwright/test'

test('landing page renders Forno brand and menu', async ({ page }) => {
  await page.goto('/')

  await expect(page.getByText(/Forno/i).first()).toBeVisible()
  await expect(page.getByText('Margherita')).toBeVisible()
  await expect(page.getByText('Diavola')).toBeVisible()
})

test('filters the menu by category', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('tab', { name: 'Drinks' }).click()

  await expect(page.getByText('House Lemonade')).toBeVisible()
  await expect(page.getByText('Margherita')).not.toBeVisible()
})

test('opens the order dialog and adds an item to the cart', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('button', { name: /Order online/i }).click()
  await page.getByRole('button', { name: /Add Margherita to order/i }).click()

  await expect(page.getByText(/1 × Margherita/)).toBeVisible()
})

test('opens the reservation dialog', async ({ page }) => {
  await page.goto('/')

  await page.getByRole('button', { name: /Request a table/i }).click()

  await expect(page.getByRole('heading', { name: /Request a table/i })).toBeVisible()
})