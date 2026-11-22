import { expect, expectAccessible, test } from '@drinkit/test-starter/playwright';
import { BORDEAUX, EMAIL, cellarBackend, member, signInByCode } from './cellar-backend.ts';

test('the sign-in page is accessible', async ({ page, goto, fakeBackend }) => {
  // Given
  cellarBackend(fakeBackend);
  await goto('/sign-in', { waitUntil: 'hydration' });

  // When & Then
  await expectAccessible(page);
});

test('the code sent by email signs the visitor in, and their cellars come in the first render', async ({ page, goto, fakeBackend }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  await goto('/sign-in', { waitUntil: 'hydration' });

  // When
  await signInByCode(page, backend);

  // Then
  await expect(page.getByText('Welcome back, Camille')).toBeVisible();
  expect(await (await page.request.get('/cellars')).text()).toContain(BORDEAUX.name);
});

test('a visitor sent to sign in comes back to the page they asked for', async ({ page, goto, fakeBackend }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  await goto('/cellars', { waitUntil: 'hydration' });
  await expect(page).toHaveURL('/sign-in?redirect=/cellars');

  // When
  await signInByCode(page, backend);

  // Then
  await expect(page).toHaveURL('/cellars');
  await expect(page.getByText(BORDEAUX.name)).toBeVisible();
});

test('a user who chose to stay signed in still is once their session ends', async ({ page, goto, fakeBackend, context }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  await goto('/sign-in', { waitUntil: 'hydration' });
  await signInByCode(page, backend, true);
  await expect(page.getByText('Welcome back, Camille')).toBeVisible();
  await context.clearCookies({ name: 'SESSION' });

  // When
  await goto('/cellars', { waitUntil: 'hydration' });

  // Then
  await expect(page.getByText(BORDEAUX.name)).toBeVisible();
});

test.describe('with a refused code', () => {
  test.use({ ignoredConsoleErrors: [/status of 401/] });

  test('a wrong code tells the visitor, who stays a visitor', async ({ page, goto, fakeBackend }) => {
    // Given
    cellarBackend(fakeBackend, member);
    await goto('/sign-in', { waitUntil: 'hydration' });
    await page.getByLabel('Email').fill(EMAIL);
    await page.getByRole('button', { name: 'Send me a code' }).click();

    // When
    await page.getByLabel('Code').fill('000000');
    await page.getByRole('button', { name: 'Sign in' }).click();

    // Then
    await expect(page.getByRole('alert')).toHaveText('This code is wrong or has expired. Check it, or ask for a new one.');
    await expect(page).toHaveURL('/sign-in');
    await expect(page.getByRole('region', { name: /Notifications/ }).getByRole('listitem')).toHaveCount(0);
  });
});
