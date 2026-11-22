import { useNuxtApp } from '#app';
import { beforeEach, expect, test } from 'vitest';
import { useFlag } from '@drinkit/feature-management-starter/use-flag';
import type { FlagKey } from '@drinkit/feature-management-starter/use-flag';
import { ofrepBackend } from './ofrep-backend.ts';
import type { OfrepBackend } from './ofrep-backend.ts';

// Cast: the keys of the app that extends the layer type its tests too
const NEW_CHECKOUT = 'new-checkout' as FlagKey;
const DARK_MODE = 'dark-mode' as FlagKey;

let backend: OfrepBackend;

beforeEach(() => {
  backend = ofrepBackend();
});

test('a flag reads the value the backend evaluated', async () => {
  // Given
  backend.flags = [{ key: 'new-checkout', value: true }];

  // When
  await useNuxtApp().callHook('drinkit:signed-in');

  // Then
  expect(useFlag(NEW_CHECKOUT).value).toBe(true);
});

test('a flag the backend did not evaluate reads its default', async () => {
  // Given
  backend.flags = [{ key: 'new-checkout', value: true }];

  // When
  await useNuxtApp().callHook('drinkit:signed-in');

  // Then
  expect(useFlag(DARK_MODE, true).value).toBe(true);
});

test('every flag reads its default while the backend cannot evaluate them', async () => {
  // Given
  backend.flags = [{ key: 'new-checkout', value: true }];
  await useNuxtApp().callHook('drinkit:signed-in');
  backend.flags = 'down';

  // When
  await useNuxtApp().callHook('drinkit:signed-out');

  // Then
  expect(useFlag(NEW_CHECKOUT).value).toBe(false);
});

test('the flags are evaluated for the keys the app declares, in one request', async () => {
  // When
  await useNuxtApp().callHook('drinkit:signed-in');

  // Then
  expect(backend.evaluations).toStrictEqual([
    { context: { flags: 'new-checkout,dark-mode', experiments: 'checkout-test' } },
  ]);
});

test('signing out reads the flags of the visitor', async () => {
  // Given
  backend.flags = [{ key: 'new-checkout', value: true }];
  await useNuxtApp().callHook('drinkit:signed-in');
  backend.flags = [{ key: 'new-checkout', value: false }];

  // When
  await useNuxtApp().callHook('drinkit:signed-out');

  // Then
  expect(useFlag(NEW_CHECKOUT).value).toBe(false);
});

test('the flags of the last subject win over an older answer that comes back after them', async () => {
  // Given
  const { promise: answered, resolve: answer } = Promise.withResolvers<null>();
  backend.slowNextAnswer = { flags: [{ key: 'new-checkout', value: true }], until: answered };
  const signedIn = useNuxtApp().callHook('drinkit:signed-in');
  backend.flags = [{ key: 'new-checkout', value: false }];
  await useNuxtApp().callHook('drinkit:signed-out');

  // When
  answer(null);
  await signedIn;

  // Then
  expect(useFlag(NEW_CHECKOUT).value).toBe(false);
});
