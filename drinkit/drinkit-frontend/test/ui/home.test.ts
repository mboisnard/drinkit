import { expectAccessible, expectGoodWebVitals, expectSameServerRender, test } from '@drinkit/test-starter/playwright';

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
