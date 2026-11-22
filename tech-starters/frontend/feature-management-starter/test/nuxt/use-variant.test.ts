import { defineComponent, h } from 'vue';
import { useNuxtApp } from '#app';
import { mountSuspended } from '@nuxt/test-utils/runtime';
import { afterEach, beforeEach, expect, test, vi } from 'vitest';
import { useVariant } from '@drinkit/feature-management-starter/use-variant';
import type { ExperimentKey } from '@drinkit/feature-management-starter/use-variant';
import { ofrepBackend } from './ofrep-backend.ts';
import type { OfrepBackend } from './ofrep-backend.ts';

// Cast: the keys of the app that extends the layer type its tests too
const CHECKOUT_TEST = 'checkout-test' as ExperimentKey;

const checkout = defineComponent({
  setup() {
    const variant = useVariant(CHECKOUT_TEST);

    return () => h('p', variant.value);
  },
});

let backend: OfrepBackend;
const mounted: Array<{ unmount: () => void }> = [];

async function mountCheckout(): Promise<void> {
  mounted.push(await mountSuspended(checkout));
}

beforeEach(() => {
  backend = ofrepBackend();
});

function unmountAll(): void {
  for (const component of mounted.splice(0)) component.unmount();
}

// A component still mounted would watch its variant in the next test
afterEach(unmountAll);

test('a variant reads the variant the backend drew', async () => {
  // Given
  backend.flags = [{ key: 'checkout-test', value: 'B', variant: 'B', reason: 'SPLIT' }];

  // When
  await useNuxtApp().callHook('drinkit:signed-in');

  // Then
  expect(useVariant(CHECKOUT_TEST).value).toBe('B');
});

test('a variant the backend did not evaluate reads control', async () => {
  // When
  await useNuxtApp().callHook('drinkit:signed-in');

  // Then
  expect(useVariant(CHECKOUT_TEST).value).toBe('control');
});

test('a variant shown to a user in the experiment reports one exposure of that variant', async () => {
  // Given
  backend.flags = [{ key: 'checkout-test', value: 'B', variant: 'B', reason: 'SPLIT' }];
  await useNuxtApp().callHook('drinkit:signed-in');

  // When
  await mountCheckout();

  // Then
  await vi.waitFor(() => {
    expect(backend.exposures).toStrictEqual([{ experiment: 'checkout-test', variant: 'B' }]);
  });
});

test('a user outside the experiment reports no exposure', async () => {
  // Given
  backend.flags = [{ key: 'checkout-test', value: 'control', variant: 'control', reason: 'DEFAULT' }];
  await useNuxtApp().callHook('drinkit:signed-in');

  // When
  await mountCheckout();

  // Then, the exposure of a user drawn next is the only one the backend gets
  unmountAll();
  backend.flags = [{ key: 'checkout-test', value: 'B', variant: 'B', reason: 'SPLIT' }];
  await useNuxtApp().callHook('drinkit:signed-in');
  await mountCheckout();
  await vi.waitFor(() => {
    expect(backend.exposures).toStrictEqual([{ experiment: 'checkout-test', variant: 'B' }]);
  });
});

test('a variant drawn again while its component stays mounted reports the exposure of the new variant', async () => {
  // Given
  backend.flags = [{ key: 'checkout-test', value: 'B', variant: 'B', reason: 'SPLIT' }];
  await useNuxtApp().callHook('drinkit:signed-in');
  await mountCheckout();
  backend.flags = [{ key: 'checkout-test', value: 'C', variant: 'C', reason: 'SPLIT' }];

  // When
  await useNuxtApp().callHook('drinkit:signed-out');

  // Then
  await vi.waitFor(() => {
    expect(backend.exposures).toStrictEqual([
      { experiment: 'checkout-test', variant: 'B' },
      { experiment: 'checkout-test', variant: 'C' },
    ]);
  });
});
