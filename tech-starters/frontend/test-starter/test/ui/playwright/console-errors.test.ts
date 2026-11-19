import { expect, test } from '@playwright/test';
import { watchConsoleErrors } from '../../../src/playwright/console-errors.ts';

test('an error logged to the console is reported', async ({ page }) => {
  // Given
  const errors = watchConsoleErrors(page, []);

  // When
  await page.setContent('<script>console.error("Hydration completed but contains mismatches.")</script>');

  // Then
  expect(errors()).toStrictEqual(['Hydration completed but contains mismatches.']);
});

test('an error thrown and never caught is reported', async ({ page }) => {
  // Given
  const errors = watchConsoleErrors(page, []);

  // When
  await page.setContent('<script>throw new Error("Cannot read the cellar")</script>');

  // Then
  expect(errors()).toStrictEqual(['Cannot read the cellar']);
});

test('an error the test ignores is not reported', async ({ page }) => {
  // Given
  const errors = watchConsoleErrors(page, [/third-party widget/]);

  // When
  await page.setContent('<script>console.error("The third-party widget failed to load")</script>');

  // Then
  expect(errors()).toStrictEqual([]);
});

test('a warning is not reported', async ({ page }) => {
  // Given
  const errors = watchConsoleErrors(page, []);

  // When
  await page.setContent('<script>console.warn("Deprecated option")</script>');

  // Then
  expect(errors()).toStrictEqual([]);
});
