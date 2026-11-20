import { clearNuxtState, useNuxtApp, useNuxtData, useRouter } from '#app';
import { beforeEach, expect, test } from 'vitest';
import { useCurrentUser } from '../../app/use-current-user.ts';
import { authBackend } from './auth-backend.ts';
import type { AuthBackend } from './auth-backend.ts';
import { addPages } from './pages.ts';

let backend: AuthBackend;

beforeEach(async () => {
  addPages();
  clearNuxtState();
  backend = authBackend();
  await useRouter().push('/');
});

test('the current user is read once from the backend, then shared by every page', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  clearNuxtState();
  backend.readsOfCurrentUser = 0;

  // When
  await useRouter().push('/private');
  await useRouter().push('/cellars');

  // Then
  expect({ user: useCurrentUser().user.value, reads: backend.readsOfCurrentUser })
    .toStrictEqual({ user: { firstname: 'Ada', roles: ['ROLE_USER'] }, reads: 1 });
});

test('a visitor the backend answers 401 is anonymous', () => {
  // When
  const { user, signedIn } = useCurrentUser();

  // Then
  expect({ user: user.value, signedIn: signedIn.value }).toStrictEqual({ user: null, signedIn: false });
});

test('the current user holds the roles the backend gives them, and no other', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  const { hasRole, refresh } = useCurrentUser();

  // When
  await refresh();

  // Then
  expect([hasRole('ROLE_USER'), hasRole('ROLE_ADMIN')]).toStrictEqual([true, false]);
});

test('refreshing reads the current user again from the backend', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_REGISTRATION_IN_PROGRESS'] };
  const { user, refresh } = useCurrentUser();
  await refresh();
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };

  // When
  await refresh();

  // Then
  expect(user.value).toStrictEqual({ firstname: 'Ada', roles: ['ROLE_USER'] });
});

test('signing out tells the backend, and the user is anonymous', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  const { signedIn, refresh, signOut } = useCurrentUser();
  await refresh();

  // When
  await signOut();

  // Then
  expect({ signedIn: signedIn.value, logouts: backend.bodies.logout?.length }).toStrictEqual({
    signedIn: false,
    logouts: 1,
  });
});

test('signing out goes to the sign-in page', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  await useRouter().push('/cellars');

  // When
  await useCurrentUser().signOut();

  // Then
  expect(useRouter().currentRoute.value.fullPath).toBe('/sign-in');
});

test('signing out clears the data the pages read for the user', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  await useRouter().push('/cellars');
  useNuxtApp().payload.data.cellars = [{ name: 'Bordeaux' }];

  // When
  await useCurrentUser().signOut();

  // Then
  expect(useNuxtData('cellars').data.value).toBeUndefined();
});

test('signing out removes the data the server rendered into the page', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  await useCurrentUser().refresh();
  const payload = document.createElement('script');
  payload.type = 'application/json';
  payload.dataset.nuxtData = 'nuxt-app';
  payload.textContent = '[{"firstname":"Ada"}]';
  document.body.append(payload);

  // When
  await useCurrentUser().signOut();

  // Then
  expect(document.body.innerHTML).not.toContain('Ada');
});

test('signing out tells the drinkit:signed-out hook', async () => {
  // Given
  backend.user = { firstname: 'Ada', roles: ['ROLE_USER'] };
  await useCurrentUser().refresh();
  let signedOut = 0;
  const unhook = useNuxtApp().hook('drinkit:signed-out', () => {
    signedOut++;
  });

  // When
  await useCurrentUser().signOut();

  // Then
  unhook();
  expect(signedOut).toBe(1);
});
