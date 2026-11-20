import { clearError, clearNuxtState, useError, useRouter } from '#app';
import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { setResponseStatus } from 'h3';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { authBackend } from '../auth-backend.ts';
import type { AuthBackend } from '../auth-backend.ts';
import { addPages } from '../pages.ts';

let backend: AuthBackend;

beforeEach(async () => {
  addPages();
  await clearError();
  clearNuxtState();
  backend = authBackend();
  await useRouter().push('/');
  clearNuxtState();
});

afterEach(() => {
  vi.restoreAllMocks();
});

test('a visitor opening a page for signed-in users goes to sign in, with the page as return URL', async () => {
  // When
  await useRouter().push('/private?tab=bottles');

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/sign-in?redirect=/private?tab=bottles');
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

test('a user without the role of a page goes to the page the app gives one of their roles', async () => {
  // Given
  backend.user = { roles: ['ROLE_REGISTRATION_IN_PROGRESS'] };

  // When
  await useRouter().push('/cellars');

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/registration');
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
    message: 'The server cannot be reached. Try again in a moment.',
  });
});

test('a backend that gives no answer at all shows that the server cannot be reached', async () => {
  // Given: what ofetch throws when nothing listens
  const noAnswer = Object.assign(new Error('[GET] "/drinkit/api/user/me": <no response> fetch failed'), { name: 'FetchError' });
  vi.spyOn(globalThis, '$fetch').mockRejectedValue(noAnswer);

  // When
  const navigation = useRouter().push('/private');

  // Then
  await expect(navigation).rejects.toMatchObject({
    status: 503,
    message: 'The server cannot be reached. Try again in a moment.',
  });
});
