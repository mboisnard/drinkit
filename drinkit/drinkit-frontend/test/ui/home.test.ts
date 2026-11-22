import { http, HttpResponse } from 'msw';
import { expect, expectAccessible, expectGoodWebVitals, expectSameServerRender, test } from '@drinkit/test-starter/playwright';
import { CURRENT_USER_PATH } from './cellar-backend.ts';

test.beforeEach(({ fakeBackend }) => {
  fakeBackend.use(http.get(CURRENT_USER_PATH, () => new HttpResponse(null, { status: 401 })));
});

test('the home page is accessible', async ({ page, goto }) => {
  // Given
  await goto('/', { waitUntil: 'hydration' });

  // When & Then
  await expectAccessible(page);
});

test('the home page shows at once and holds still', async ({ page, goto }) => {
  // Given
  await goto('/', { waitUntil: 'hydration' });

  // When & Then
  await expectGoodWebVitals(page);
});

test('the home page renders the same for every request', async ({ request }) => {
  // When & Then
  await expectSameServerRender(request, '/');
});

test('the home page greets the signed-in user by first name', async ({ page, goto, fakeBackend }) => {
  // Given
  fakeBackend.use(http.get(CURRENT_USER_PATH, () => HttpResponse.json({ firstname: 'Camille', roles: ['ROLE_USER'] })));

  // When
  await goto('/', { waitUntil: 'hydration' });

  // Then
  await expect(page.getByText('Welcome back, Camille')).toBeVisible();
});

test('the home page greets no one when no one is signed in', async ({ page, goto }) => {
  // When
  await goto('/', { waitUntil: 'hydration' });

  // Then
  await expect(page.getByRole('heading', { level: 1 })).toHaveText('DrinkIt');
  await expect(page.getByText('Welcome back')).toHaveCount(0);
});

test.describe('with a backend that fails', () => {
  test.use({ ignoredConsoleErrors: [/status of 500/] });

  test('the home page shows the error of a backend that fails', async ({ page, goto, fakeBackend }) => {
    // Given
    fakeBackend.use(http.get(CURRENT_USER_PATH, () => HttpResponse.json(
      { title: 'Internal Server Error', status: 500 },
      { status: 500, headers: { 'content-type': 'application/problem+json' } },
    )));

    // When
    await goto('/', { waitUntil: 'hydration' });

    // Then
    await expect(page.getByRole('heading', { level: 1 })).toHaveText('Internal Server Error');
  });
});
