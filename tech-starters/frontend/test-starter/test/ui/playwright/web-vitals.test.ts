import { expect, test } from '@playwright/test';
import { http, HttpResponse } from 'msw';
import { startFakeBackend } from '../../../src/fake-backend/index.ts';
import type { FakeBackend } from '../../../src/fake-backend/index.ts';
import { expectGoodWebVitals } from '../../../src/playwright/index.ts';

function cellarsPage(body: string): string {
  return `<!doctype html><html lang="en"><head><title>Cellars</title></head>
  <body><main><h1>Cellars</h1>${body}</main></body></html>`;
}

let server: FakeBackend | undefined;

test.afterEach(async () => {
  await server?.close();
});

test('a page that shows its content at once and holds still passes', async ({ page }) => {
  // Given
  server = await startFakeBackend(http.get('/cellars', () => HttpResponse.html(cellarsPage('<p>No cellar yet.</p>'))));
  await page.goto(`${server.url}/cellars`);

  // When & Then
  await expectGoodWebVitals(page);
});

test('a page whose layout shifts under the reader fails on CLS', async ({ page }) => {
  // Given
  const banner = `<script>setTimeout(() => document.querySelector('main')
    .prepend(Object.assign(document.createElement('div'), { style: 'height: 600px' })), 100)</script>`;
  server = await startFakeBackend(http.get('/cellars', () => HttpResponse.html(cellarsPage(`<p>No cellar yet.</p>${banner}`))));
  await page.goto(`${server.url}/cellars`);
  await page.locator('main > div').waitFor();

  // When & Then
  await expect(expectGoodWebVitals(page)).rejects.toThrow('CLS');
});

test('a page whose main content shows after 2.5 s fails on LCP', async ({ page }) => {
  // Given
  const late = `<script>setTimeout(() => document.querySelector('main')
    .append(Object.assign(document.createElement('p'), { textContent: 'Wine '.repeat(400) })), 2600)</script>`;
  server = await startFakeBackend(http.get('/cellars', () => HttpResponse.html(cellarsPage(late))));
  await page.goto(`${server.url}/cellars`);
  await page.getByText('Wine Wine').waitFor();

  // When & Then
  await expect(expectGoodWebVitals(page)).rejects.toThrow('LCP');
});
