import type { Page } from '@playwright/test';
import { expect, test } from '@drinkit/test-starter/playwright';
import { cellarBackend } from './cellar-backend.ts';
import type { CellarBackend, User } from './cellar-backend.ts';

const EMAIL = 'camille@drinkit.dev';
const DRAWN_WELCOME = { key: 'cellars-heading', value: 'welcome', variant: 'welcome', reason: 'SPLIT' };
const COUNT_ON = { key: 'cellar-count', value: true };

function member(): User {
  return { firstname: 'Camille', roles: ['ROLE_USER'] };
}

async function signIn(page: Page, backend: CellarBackend): Promise<void> {
  await page.getByLabel('Email').fill(EMAIL);
  await page.getByRole('button', { name: 'Send me a code' }).click();
  await expect(page.getByLabel('Code')).toBeVisible();
  await page.getByLabel('Code').fill(backend.lastCodeOf(EMAIL) ?? '');
  await page.getByRole('button', { name: 'Sign in' }).click();
}

// The requests of the browser only: those of the server render never reach the page
function ofrepRequestsOf(page: Page): string[] {
  const requests: string[] = [];
  page.on('request', (request) => {
    if (request.url().includes('/ofrep/')) {
      requests.push(request.url());
    }
  });

  return requests;
}

test.describe('signed in', () => {
  let backend: CellarBackend;

  test.beforeEach(async ({ context, fakeBackend, baseURL }) => {
    backend = cellarBackend(fakeBackend);
    const cookies = backend.signedInAs(member());
    await context.addCookies(cookies.map((cookie) => ({ ...cookie, url: baseURL })));
  });

  test('the server renders the variant the backend drew, and the browser reads no flag again', async ({ page, goto }) => {
    // Given
    backend.flags.user = [DRAWN_WELCOME];
    const ofrepRequests = ofrepRequestsOf(page);

    // When
    await goto('/cellars', { waitUntil: 'hydration' });

    // Then
    await expect(page.getByRole('heading', { name: 'Welcome to your cellars' })).toBeVisible();
    await expect(page).toHaveTitle(/Welcome to your cellars/);
    expect(ofrepRequests).toStrictEqual([]);
  });

  test('a flag on shows what it guards', async ({ page, goto }) => {
    // Given
    backend.flags.user = [COUNT_ON];

    // When
    await goto('/cellars', { waitUntil: 'hydration' });

    // Then
    await expect(page.getByText('1 cellar')).toBeVisible();
  });

  test('a variant shown reports one exposure, and its server render none', async ({ goto }) => {
    // Given
    backend.flags.user = [DRAWN_WELCOME];

    // When
    await goto('/cellars', { waitUntil: 'hydration' });

    // Then
    await expect.poll(() => backend.exposures).toStrictEqual([{ experiment: 'cellars-heading', variant: 'welcome' }]);
  });

  test('the flags leave nothing in the local storage of the browser', async ({ page, goto }) => {
    // Given
    backend.flags.user = [DRAWN_WELCOME, COUNT_ON];

    // When
    await goto('/cellars', { waitUntil: 'hydration' });

    // Then
    await expect.poll(() => backend.exposures).toHaveLength(1);
    expect(await page.evaluate(() => Object.keys(localStorage))).toStrictEqual([]);
  });

  test('signing out reads the flags of the visitor, without reloading the page', async ({ page, goto }) => {
    // Given
    backend.flags.user = [COUNT_ON];
    backend.flags.visitor = [{ key: 'cellar-count', value: false }];
    await goto('/cellars', { waitUntil: 'hydration' });
    const documents: string[] = [];
    page.on('request', (request) => {
      if (request.resourceType() === 'document') {
        documents.push(request.url());
      }
    });

    // When
    const evaluation = page.waitForResponse((response) => response.url().includes('/ofrep/'));
    await page.getByRole('button', { name: 'Sign out' }).click();

    // Then
    expect(await (await evaluation).json()).toStrictEqual({ flags: [{ key: 'cellar-count', value: false }] });
    await expect(page).toHaveURL('/sign-in');
    expect(documents).toStrictEqual([]);
  });
});

test('signing in shows the flags of the user, without reloading the page', async ({ page, goto, fakeBackend }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  backend.flags.user = [COUNT_ON];
  await goto('/sign-in?redirect=/cellars', { waitUntil: 'hydration' });
  const documents: string[] = [];
  page.on('request', (request) => {
    if (request.resourceType() === 'document') {
      documents.push(request.url());
    }
  });

  // When
  await signIn(page, backend);

  // Then
  await expect(page.getByText('1 cellar')).toBeVisible();
  expect(documents).toStrictEqual([]);
});
