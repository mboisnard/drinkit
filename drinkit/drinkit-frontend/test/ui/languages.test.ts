import { http, HttpResponse } from 'msw';
import { expect, expectAccessible, expectSameServerRender, test } from '@drinkit/test-starter/playwright';
import { CURRENT_USER_PATH, EMAIL, cellarBackend, member } from './cellar-backend.ts';

function langOf(html: string): string | undefined {
  return /<html[^>]* lang="([^"]+)"/.exec(html)?.[1];
}

function alternatesOf(html: string): string[] {
  return [...html.matchAll(/hreflang="([^"]+)"/g)].map(([, language]) => language ?? '').sort();
}

test.describe('a visitor', () => {
  test.beforeEach(({ fakeBackend }) => {
    fakeBackend.use(http.get(CURRENT_USER_PATH, () => new HttpResponse(null, { status: 401 })));
  });

  test('a page names its language, and its versions in the other languages, in the HTML the server renders', async ({ request }) => {
    // When
    const [english, french] = await Promise.all([request.get('/'), request.get('/fr')]);

    // Then
    const [englishHtml, frenchHtml] = [await english.text(), await french.text()];
    expect({ english: langOf(englishHtml), french: langOf(frenchHtml), alternates: alternatesOf(frenchHtml) })
      .toStrictEqual({ english: 'en-GB', french: 'fr-FR', alternates: ['en', 'en-GB', 'fr', 'fr-FR', 'x-default'] });
  });

  test('a translated page renders the same for every request, so the browser takes it over without a mismatch', async ({ request }) => {
    // When & Then
    await expectSameServerRender(request, '/fr');
  });

  test('a translated page stays accessible', async ({ page, goto }) => {
    // Given
    await goto('/fr', { waitUntil: 'hydration' });

    // When & Then
    await expectAccessible(page);
  });
});

test.describe('a page the app does not have', () => {
  // The browser reports the status of the page it was served
  test.use({ ignoredConsoleErrors: [/status of 404/] });

  test('says so in the language of its path', async ({ page, goto, fakeBackend }) => {
    // Given
    fakeBackend.use(http.get(CURRENT_USER_PATH, () => HttpResponse.json(member())));

    // When
    await goto('/fr/nowhere', { waitUntil: 'hydration' });

    // Then
    await expect(page.getByRole('heading', { level: 1, name: 'Page introuvable' })).toBeVisible();
  });
});

test.describe('with a backend that is down', () => {
  test.use({ ignoredConsoleErrors: [/status of 503/] });

  test('the error page says that the server cannot be reached in the language of the page', async ({ page, goto, fakeBackend }) => {
    // Given
    fakeBackend.use(http.get(CURRENT_USER_PATH, () => new HttpResponse(null, { status: 502 })));

    // When
    await goto('/fr', { waitUntil: 'hydration' });

    // Then
    await expect(page.getByRole('heading', { level: 1, name: 'Service indisponible' })).toBeVisible();
    await expect(page.getByText('Le serveur est injoignable. Réessayez dans un instant.')).toBeVisible();
  });
});

test('a visitor signs in in French, and every call asks the backend for French', async ({ page, goto, fakeBackend }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  const languages = new Set<string | null>();
  fakeBackend.use(http.all('/drinkit/*', ({ request }) => {
    languages.add(request.headers.get('accept-language'));
  }));
  await goto('/fr/sign-in', { waitUntil: 'hydration' });

  // When
  await page.getByLabel('E-mail').fill(EMAIL);
  await page.getByRole('button', { name: 'Recevoir un code' }).click();
  await expect(page.getByLabel('Code', { exact: true })).toBeVisible();
  await page.getByLabel('Code', { exact: true }).fill(backend.lastCodeOf(EMAIL) ?? '');
  await page.getByRole('button', { name: 'Se connecter' }).click();

  // Then
  await expect(page).toHaveURL('/fr');
  await expect(page.getByText('Bon retour, Camille')).toBeVisible();
  expect([...languages]).toStrictEqual(['fr-FR']);
});

test('a member reads the pages without a prefix in the language of their account, and the public ones in that of the address', async ({ page, goto, context, fakeBackend, baseURL }) => {
  // Given
  await cellarBackend(fakeBackend).openSession(context, baseURL, { ...member(), locale: 'fr' });

  // When
  await goto('/cellars', { waitUntil: 'hydration' });
  const cellars = {
    title: await page.getByRole('heading', { level: 1 }).textContent(),
    lang: await page.locator('html').getAttribute('lang'),
    alternates: await page.locator('link[hreflang]').count(),
  };
  await goto('/', { waitUntil: 'hydration' });

  // Then
  expect({ cellars, home: await page.locator('html').getAttribute('lang') })
    .toStrictEqual({ cellars: { title: 'Mes caves', lang: 'fr-FR', alternates: 0 }, home: 'en-GB' });
  expect((await page.request.get('/fr/cellars')).status()).toBe(404);
});
