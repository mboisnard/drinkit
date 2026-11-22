import { clearNuxtState, useNuxtApp, useRouter } from '#app';
import { beforeEach, expect, test } from 'vitest';
import { isLocaleState } from '@drinkit/i18n-starter/locale-state';
import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
import { authBackend } from '../auth-backend.ts';
import type { AuthBackend } from '../auth-backend.ts';
import { addPages } from '../pages.ts';

let backend: AuthBackend;

beforeEach(async () => {
  addPages();
  clearNuxtState((key) => !isLocaleState(key));
  backend = authBackend();
  await useRouter().push('/');
});

test('a 401 while signed in forgets the user, who signs in again and comes back', async () => {
  // Given
  backend.user = { roles: ['ROLE_USER'] };
  await useCurrentUser().refresh();
  await useRouter().push('/cellars');

  // When
  await useNuxtApp().callHook('drinkit:api:problem', { status: 401, title: 'Unauthorized' });

  // Then
  expect({ signedIn: useCurrentUser().signedIn.value, at: useRouter().currentRoute.value.fullPath })
    .toStrictEqual({ signedIn: false, at: '/sign-in?redirect=/cellars' });
});

test('a 401 sends the user to sign in again in the language of their account', async () => {
  // Given
  backend.user = { roles: ['ROLE_USER'], locale: 'fr' };
  await useCurrentUser().refresh();
  await useRouter().push('/cellars');

  // When
  await useNuxtApp().callHook('drinkit:api:problem', { status: 401, title: 'Unauthorized' });

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/fr/sign-in?redirect=/cellars');
  await useNuxtApp().$i18n.setLocale('en');
});

test('a 401 to a visitor, such as a refused code, changes nothing', async () => {
  // Given
  await useRouter().push('/sign-in');

  // When
  await useNuxtApp().callHook('drinkit:api:problem', { status: 401, title: 'Unauthorized' });

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/sign-in');
});
