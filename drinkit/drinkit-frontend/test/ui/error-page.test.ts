import { http, HttpResponse } from 'msw';
import { expect, expectAccessible, test } from '@drinkit/test-starter/playwright';

// The browser reports the status of the page it was served
test.use({ ignoredConsoleErrors: [/status of 503/] });

test.beforeEach(({ fakeBackend }) => {
  // What a gateway answers for a backend that is down
  fakeBackend.use(http.get('/drinkit/api/user/me', () => new HttpResponse(null, { status: 502 })));
});

test('without a backend, the error page says the server cannot be reached, with its status and a way out', async ({ page, goto }) => {
  // When
  await goto('/', { waitUntil: 'hydration' });

  // Then
  await expect(page.getByText('503', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { level: 1, name: 'Service Unavailable' })).toBeVisible();
  await expect(page.getByText('The server cannot be reached. Try again in a moment.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Back to home' })).toBeVisible();
});

test('the error page is accessible', async ({ page, goto }) => {
  // Given
  await goto('/', { waitUntil: 'hydration' });

  // When & Then
  await expectAccessible(page);
});

test('a backend that hangs is waited for once, until the timeout, before the page says the server cannot be reached', async ({ page, goto, fakeBackend }) => {
  // Given
  let readsOfUser = 0;
  fakeBackend.use(http.get('/drinkit/api/user/me', async () => {
    readsOfUser++;
    await new Promise(() => undefined);
  }));

  // When
  await goto('/', { waitUntil: 'hydration' });

  // Then
  await expect(page.getByText('The server cannot be reached. Try again in a moment.')).toBeVisible();
  expect(readsOfUser).toBe(1);
});
