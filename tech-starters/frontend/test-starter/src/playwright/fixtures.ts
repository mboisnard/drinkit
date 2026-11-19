import { expect, test as nuxtTest } from '@nuxt/test-utils/playwright';
import { watchConsoleErrors } from './console-errors.ts';

export { expect };

/** The test of `@nuxt/test-utils`, which fails when the page logs an error to the console or throws one. */
export const test = nuxtTest.extend<{ ignoredConsoleErrors: RegExp[]; noConsoleError: undefined }>({
  ignoredConsoleErrors: [[], { option: true }],
  noConsoleError: [async ({ page, ignoredConsoleErrors }, use) => {
    const errors = watchConsoleErrors(page, ignoredConsoleErrors);

    await use(undefined);

    expect(errors(), 'The page logged errors to the console').toStrictEqual([]);
  }, { auto: true }],
});
