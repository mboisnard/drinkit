import { expect, test } from '@playwright/test';
import { expectAccessible } from '../../../src/playwright/index.ts';

const ACCESSIBLE_PAGE = `<!doctype html><html lang="en"><head><title>Cellars</title></head>
  <body><main><h1>Cellars</h1><p>No cellar yet.</p></main></body></html>`;

test('a page that meets WCAG 2.2 AA passes', async ({ page }) => {
  // Given
  await page.setContent(ACCESSIBLE_PAGE);

  // When & Then
  await expectAccessible(page);
});

test('a page that breaks a WCAG rule fails with the rule broken', async ({ page }) => {
  // Given
  await page.setContent(ACCESSIBLE_PAGE.replace('<p>No cellar yet.</p>', '<img src="bottle.png">'));

  // When & Then
  await expect(expectAccessible(page)).rejects.toThrow('image-alt');
});

test('a page without a level one heading fails', async ({ page }) => {
  // Given
  await page.setContent(ACCESSIBLE_PAGE.replace('<h1>Cellars</h1>', '<p>Cellars</p>'));

  // When & Then
  await expect(expectAccessible(page)).rejects.toThrow('page-has-heading-one');
});
