import { expect, expectAccessible, test } from '@drinkit/test-starter/playwright';
import { BORDEAUX, cellarBackend } from './cellar-backend.ts';

test.beforeEach(async ({ context, fakeBackend, baseURL }) => {
  const backend = cellarBackend(fakeBackend);
  const cookies = backend.signedInAs({ firstname: 'Camille', roles: ['ROLE_USER'] });
  await context.addCookies(cookies.map((cookie) => ({ ...cookie, url: baseURL })));
});

test('the cellars page is accessible', async ({ page, goto }) => {
  // Given
  await goto('/cellars', { waitUntil: 'hydration' });

  // When & Then
  await expectAccessible(page);
});

test('after sign-out, the back button shows no cellar', async ({ page, goto }) => {
  // Given
  await goto('/cellars', { waitUntil: 'hydration' });
  await expect(page.getByText(BORDEAUX.name)).toBeVisible();
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL('/sign-in');

  // When
  await page.goBack();

  // Then
  await expect(page).toHaveURL('/sign-in?redirect=/cellars');
  await expect(page.getByText(BORDEAUX.name)).toHaveCount(0);
});

test('after sign-out, opening the cellars again asks to sign in', async ({ page, goto }) => {
  // Given
  await goto('/cellars', { waitUntil: 'hydration' });
  await page.getByRole('button', { name: 'Sign out' }).click();
  await expect(page).toHaveURL('/sign-in');

  // When
  await goto('/cellars', { waitUntil: 'hydration' });

  // Then
  await expect(page).toHaveURL('/sign-in?redirect=/cellars');
  await expect(page.getByText(BORDEAUX.name)).toHaveCount(0);
});
