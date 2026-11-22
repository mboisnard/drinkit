import { expect, test as nuxtTest } from '@nuxt/test-utils/playwright';
import { startFakeBackend } from '../fake-backend/index.ts';
import type { FakeBackend } from '../fake-backend/index.ts';
import { watchConsoleErrors } from './console-errors.ts';

export { expect };

interface TestFixtures {
  ignoredConsoleErrors: RegExp[];
  noConsoleError: undefined;
  /** The backend the server of the app calls, whose handlers a test adds with `use` for itself alone. */
  fakeBackend: FakeBackend;
}

interface WorkerFixtures {
  workerFakeBackend: FakeBackend;
}

/**
 * The test of `@nuxt/test-utils`, which fails when the page logs an error to the console or throws one, on an app
 * whose server calls a fake backend: started before the app, it reaches the server as the backend URL of
 * `@drinkit/api-client-starter`.
 */
export const test = nuxtTest.extend<TestFixtures, WorkerFixtures>({
  ignoredConsoleErrors: [[], { option: true }],
  noConsoleError: [async ({ page, ignoredConsoleErrors }, use) => {
    const errors = watchConsoleErrors(page, ignoredConsoleErrors);

    await use(undefined);

    expect(errors(), 'The page logged errors to the console').toStrictEqual([]);
  }, { auto: true }],
  // eslint-disable-next-line no-empty-pattern -- Playwright reads from this pattern the fixtures it needs: none here
  workerFakeBackend: [async ({}, use) => {
    const backend = await startFakeBackend();

    await use(backend);

    await backend.close();
  }, { scope: 'worker' }],
  defaults: [async ({ workerFakeBackend }, use) => {
    await use({ nuxt: { env: { NUXT_API_CLIENT_BACKEND_URL: workerFakeBackend.url } } });
  }, { scope: 'worker' }],
  fakeBackend: async ({ workerFakeBackend }, use) => {
    await use(workerFakeBackend);

    workerFakeBackend.reset();
  },
});
