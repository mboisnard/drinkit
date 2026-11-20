import { expect, expectAccessible, test } from '@drinkit/test-starter/playwright';
import { BORDEAUX, cellarBackend } from './cellar-backend.ts';

test.beforeEach(async ({ context, fakeBackend, baseURL }) => {
  const backend = cellarBackend(fakeBackend);
  const cookies = backend.signedInAs({ roles: ['ROLE_REGISTRATION_IN_PROGRESS'] });
  await context.addCookies(cookies.map((cookie) => ({ ...cookie, url: baseURL })));
});

test('the registration page is accessible', async ({ page, goto }) => {
  // Given
  await goto('/registration', { waitUntil: 'hydration' });

  // When & Then
  await expectAccessible(page);
});

test('a user still registering who opens the cellars goes to the registration', async ({ page, goto }) => {
  // When
  await goto('/cellars', { waitUntil: 'hydration' });

  // Then
  await expect(page).toHaveURL('/registration');
});

test('a user who completes the registration reaches their cellars', async ({ page, goto }) => {
  // Given
  await goto('/registration', { waitUntil: 'hydration' });
  await page.getByLabel('First name').fill('Camille');
  await page.getByLabel('Last name').fill('Martin');
  await page.getByLabel('Birth date').fill('1990-04-12');

  // When
  await page.getByRole('button', { name: 'Continue' }).click();

  // Then
  await expect(page).toHaveURL('/cellars');
  await expect(page.getByText(BORDEAUX.name)).toBeVisible();
});
