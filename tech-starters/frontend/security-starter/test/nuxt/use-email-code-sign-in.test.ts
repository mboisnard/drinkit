import { clearNuxtState, useNuxtApp, useRouter } from '#app';
import { beforeEach, expect, test } from 'vitest';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { isLocaleState } from '@drinkit/i18n-starter/locale-state';
import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
import { useEmailCodeSignIn } from '@drinkit/security-starter/use-email-code-sign-in';
import { authBackend } from './auth-backend.ts';
import type { AuthBackend } from './auth-backend.ts';
import { addPages } from './pages.ts';

let backend: AuthBackend;

beforeEach(async () => {
  addPages();
  clearNuxtState((key) => !isLocaleState(key));
  backend = authBackend();
  backend.signsIn = { firstname: 'Ada', roles: ['ROLE_USER'] };
  await useRouter().push('/sign-in');
});

test('asking for a code sends the email to the backend', async () => {
  // When
  await useEmailCodeSignIn().requestCode('ada@drinkit.com');

  // Then
  expect(backend.bodies.code).toStrictEqual([{ email: 'ada@drinkit.com' }]);
});

test('the right code signs the visitor in', async () => {
  // When
  await useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '123456', rememberMe: false });

  // Then
  expect(useCurrentUser().user.value).toStrictEqual({ firstname: 'Ada', roles: ['ROLE_USER'] });
});

test('signing in asks the backend to remember the user when the visitor chose it', async () => {
  // When
  await useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '123456', rememberMe: true });

  // Then
  expect(backend.bodies['sign-in']).toStrictEqual([{ email: 'ada@drinkit.com', code: '123456', rememberMe: true }]);
});

test('a signed-in visitor goes back to the page they asked for', async () => {
  // Given
  await useRouter().push('/sign-in?redirect=/cellars');

  // When
  await useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '123456', rememberMe: false });

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/cellars');
});

test('a sign-in without a return URL ends on the home page of the language it was made in', async () => {
  // Given
  await useNuxtApp().$i18n.setLocale('fr');
  await useRouter().push('/fr/sign-in');

  // When
  await useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '123456', rememberMe: false });

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/fr');
  await useNuxtApp().$i18n.setLocale('en');
});

test('a return URL to another origin is ignored, and the visitor goes to the home page', async () => {
  // Given
  await useRouter().push('/sign-in?redirect=https://evil.example/cellars');

  // When
  await useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '123456', rememberMe: false });

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/');
});

test('signing in tells the drinkit:signed-in hook once the user is known', async () => {
  // Given
  const known: unknown[] = [];
  const unhook = useNuxtApp().hook('drinkit:signed-in', () => {
    known.push(useCurrentUser().user.value);
  });

  // When
  await useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '123456', rememberMe: false });

  // Then
  unhook();
  expect(known).toStrictEqual([{ firstname: 'Ada', roles: ['ROLE_USER'] }]);
});

test('a refused code throws the problem of the backend, and the visitor stays anonymous', async () => {
  // When
  const signIn = useEmailCodeSignIn().signIn({ email: 'ada@drinkit.com', code: '000000', rememberMe: false });

  // Then
  await expect(signIn).rejects.toBeInstanceOf(ProblemError);
  expect(useCurrentUser().signedIn.value).toBe(false);
});
