import type { Page } from '@playwright/test';

/** Collects what the page logs as an error and what it throws without catching, except the messages ignored. */
export function watchConsoleErrors(page: Page, ignored: RegExp[]): () => string[] {
  const errors: string[] = [];

  page.on('console', (message) => {
    if (message.type() === 'error') {
      errors.push(message.text());
    }
  });
  page.on('pageerror', (error) => {
    errors.push(error.message);
  });

  return () => errors.filter((message) => !ignored.some((pattern) => pattern.test(message)));
}
