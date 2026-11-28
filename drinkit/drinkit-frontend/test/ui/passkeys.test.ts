import { expect, expectAccessible, test as uiTest } from '@drinkit/test-starter/playwright';
import { cellarBackend, member, signInByCode } from './cellar-backend.ts';

const test = uiTest.extend<{ virtualAuthenticator: undefined }>({
  // A browser refuses an IP address as the relying party id of a passkey: the same server, by its name
  baseURL: async ({ baseURL }, use) => {
    await use(baseURL?.replace('127.0.0.1', 'localhost'));
  },
  // The authenticator of a laptop, that verifies the user, and picks the passkey in the browser's dialog for them
  virtualAuthenticator: [async ({ page }, use) => {
    const cdp = await page.context().newCDPSession(page);
    await cdp.send('WebAuthn.enable', { enableUI: false });
    await cdp.send('WebAuthn.addVirtualAuthenticator', {
      options: {
        protocol: 'ctap2',
        transport: 'internal',
        hasResidentKey: true,
        hasUserVerification: true,
        isUserVerified: true,
        automaticPresenceSimulation: true,
      },
    });

    await use(undefined);
  }, { auto: true }],
});

test('a user who accepts the passkey offer signs in with it, without typing an email', async ({ page, goto, context, fakeBackend }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  await goto('/sign-in', { waitUntil: 'hydration' });
  await signInByCode(page, backend);
  await page.getByRole('button', { name: 'Create a passkey' }).click();
  await expect(page.getByRole('region', { name: /Notifications/ })).toContainText('Passkey created');
  await context.clearCookies();
  await goto('/sign-in', { waitUntil: 'hydration' });

  // When
  await page.getByRole('button', { name: 'Sign in with a passkey' }).click();

  // Then
  await expect(page.getByText('Welcome back, Camille')).toBeVisible();
});

test('a device offered a passkey once is not offered it again at the next sign-in by code', async ({ page, goto, context, fakeBackend }) => {
  // Given
  const backend = cellarBackend(fakeBackend, member);
  await goto('/sign-in', { waitUntil: 'hydration' });
  await signInByCode(page, backend);
  await expect(page.getByRole('button', { name: 'Create a passkey' })).toBeVisible();
  await context.clearCookies();
  await goto('/sign-in', { waitUntil: 'hydration' });

  // When
  await signInByCode(page, backend);

  // Then
  await expect(page.getByText('Welcome back, Camille')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Create a passkey' })).toHaveCount(0);
});

test.describe('on the My passkeys page', () => {
  test('the page is accessible', async ({ page, goto, context, fakeBackend, baseURL }) => {
    // Given
    await cellarBackend(fakeBackend).openSession(context, baseURL, member());
    await goto('/passkeys', { waitUntil: 'hydration' });

    // When & Then
    await expectAccessible(page);
  });

  test.describe('with a removed passkey', () => {
    test.use({ ignoredConsoleErrors: [/status of 401/] });

    test('a removed passkey no longer signs in', async ({ page, goto, context, fakeBackend, baseURL }) => {
      // Given
      await cellarBackend(fakeBackend).openSession(context, baseURL, member());
      await goto('/passkeys', { waitUntil: 'hydration' });
      await page.getByRole('button', { name: 'Add a passkey' }).click();
      await page.getByRole('button', { name: /^Remove Passkey of / }).click();
      await expect(page.getByText('You have no passkey yet.')).toBeVisible();
      await context.clearCookies();
      await goto('/sign-in', { waitUntil: 'hydration' });

      // When
      await page.getByRole('button', { name: 'Sign in with a passkey' }).click();

      // Then
      await expect(page.getByRole('alert')).toHaveText('This passkey was removed from your account. Sign in with a code instead.');
      await expect(page).toHaveURL('/sign-in');
    });
  });
});

test.describe('in a browser without WebAuthn', () => {
  test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => Reflect.deleteProperty(window, 'PublicKeyCredential'));
  });

  test('the sign-in page shows no passkey button', async ({ page, goto, fakeBackend }) => {
    // Given
    cellarBackend(fakeBackend, member);

    // When
    await goto('/sign-in', { waitUntil: 'hydration' });

    // Then
    await expect(page.getByRole('button', { name: 'Send me a code' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in with a passkey' })).toHaveCount(0);
  });

  test('the sign-in by code offers no passkey', async ({ page, goto, fakeBackend }) => {
    // Given
    const backend = cellarBackend(fakeBackend, member);
    await goto('/sign-in', { waitUntil: 'hydration' });

    // When
    await signInByCode(page, backend);

    // Then
    await expect(page.getByText('Welcome back, Camille')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create a passkey' })).toHaveCount(0);
  });
});
