import { http, HttpResponse } from 'msw';
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

test.describe('with a sign-out that fails', () => {
  // One pattern: Playwright reads a second item of the array as the options of the fixture
  test.use({ ignoredConsoleErrors: [/status of 500|Unhandled error/] });

  test('a failed sign-out shows its message and the reference of the error in an accessible toast', async ({ page, goto, fakeBackend }) => {
    // Given
    fakeBackend.use(http.post('/drinkit/api/auth/logout', () => HttpResponse.json(
      { title: 'Internal Server Error', status: 500, traceId: '4bf92f3577b34da6' },
      { status: 500, headers: { 'content-type': 'application/problem+json' } },
    )));
    await goto('/cellars', { waitUntil: 'hydration' });

    // When
    await page.getByRole('button', { name: 'Sign out' }).click();

    // Then
    const notifications = page.getByRole('region', { name: /Notifications/ });
    await expect(notifications).toContainText('Sign-out failed. Try again.');
    await expect(notifications).toContainText('Reference: 4bf92f3577b34da6');
    await expectAccessible(page);
  });
});
