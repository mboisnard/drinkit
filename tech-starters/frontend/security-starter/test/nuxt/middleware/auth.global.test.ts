import { clearError, clearNuxtState, useError, useNuxtApp, useRouter } from '#app';
import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { setResponseStatus } from 'h3';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { isLocaleState } from '@drinkit/i18n-starter/locale-state';
import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';
import { authBackend } from '../auth-backend.ts';
import type { AuthBackend } from '../auth-backend.ts';
import { addPages } from '../pages.ts';

let backend: AuthBackend;

beforeEach(async () => {
  addPages();
  await clearError();
  clearNuxtState((key) => !isLocaleState(key));
  backend = authBackend();
  await useRouter().push('/');
  clearNuxtState((key) => !isLocaleState(key));
});

afterEach(async () => {
  vi.restoreAllMocks();
  await useNuxtApp().$i18n.setLocale('en');
});

test('a visitor opening a page for signed-in users goes to sign in, with the page as return URL', async () => {
  // When
  await useRouter().push('/private?tab=bottles');

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/sign-in?redirect=/private?tab=bottles');
});

test('a visitor sent to sign in keeps the language they were reading', async () => {
  // Given
  await useNuxtApp().$i18n.setLocale('fr');

  // When
  await useRouter().push('/private?tab=bottles');

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/fr/sign-in?redirect=/private?tab=bottles');
});

test('a user holding the role of a page opens it', async () => {
  // Given
  backend.user = { roles: ['ROLE_USER'] };

  // When
  await useRouter().push('/cellars');

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/cellars');
});

test('a user without the role of a page gets a 403', async () => {
  // Given
  backend.user = { roles: ['ROLE_USER'] };

  // When
  const navigation = useRouter().push('/admin');

  // Then
  await expect(navigation).rejects.toMatchObject({ status: 403 });
  expect({ at: useRouter().currentRoute.value.path, shown: useError().value?.status }).toStrictEqual({ at: '/', shown: 403 });
});

test('a page refused to an account says so in the language of the account', async () => {
  // Given
  backend.user = { roles: ['ROLE_USER'], locale: 'fr' };

  // When
  const navigation = useRouter().push('/admin');

  // Then
  await expect(navigation).rejects.toMatchObject({ status: 403 });
  expect(toProblemDetail(useError().value ?? {})).toStrictEqual({ title: 'Accès refusé', status: 403 });
});

test('a user without the role of a page goes to the page the app gives one of their roles', async () => {
  // Given
  backend.user = { roles: ['ROLE_REGISTRATION_IN_PROGRESS'] };

  // When
  await useRouter().push('/cellars');

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/registration');
});

test('a page for signed-in users shows in the language of the account, whatever the page before', async () => {
  // Given
  backend.user = { roles: ['ROLE_USER'], locale: 'fr' };

  // When
  await useRouter().push('/cellars');

  // Then
  expect({ page: useRouter().currentRoute.value.fullPath, locale: useNuxtApp().$i18n.locale.value })
    .toStrictEqual({ page: '/cellars', locale: 'fr' });
});

test('a backend that cannot tell who the user is opens no page for signed-in users', async () => {
  // Given
  registerEndpoint('/drinkit/api/user/me', {
    method: 'GET',
    handler: (event) => {
      setResponseStatus(event, 503, 'Service Unavailable');

      return { title: 'Service Unavailable', status: 503 };
    },
  });

  // When
  const navigation = useRouter().push('/private');

  // Then
  await expect(navigation).rejects.toMatchObject({ status: 503 });
  expect(useRouter().currentRoute.value.path).toBe('/');
});

test.each([502, 504])('a backend a gateway gets no answer from, %i, shows that the server cannot be reached', async (status) => {
  // Given
  registerEndpoint('/drinkit/api/user/me', {
    method: 'GET',
    handler: (event) => {
      setResponseStatus(event, status);

      return null;
    },
  });

  // When
  const navigation = useRouter().push('/private');

  // Then
  await expect(navigation).rejects.toMatchObject({
    status: 503,
    data: { title: 'Service Unavailable', detail: 'The server cannot be reached. Try again in a moment.' },
  });
});

test('a backend that gives no answer shows that the server cannot be reached in the language of the page', async () => {
  // Given: what ofetch throws when nothing listens
  await useNuxtApp().$i18n.setLocale('fr');
  clearNuxtState((key) => !isLocaleState(key));
  vi.spyOn(globalThis, '$fetch').mockRejectedValue(Object.assign(new Error('fetch failed'), { name: 'FetchError' }));

  // When
  const navigation = useRouter().push('/private');

  // Then
  await expect(navigation).rejects.toMatchObject({
    status: 503,
    data: { title: 'Service indisponible', detail: 'Le serveur est injoignable. Réessayez dans un instant.' },
  });
});
