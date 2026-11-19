import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import type * as WebVitals from 'web-vitals';

// The thresholds Google rates good, https://web.dev/articles/vitals
const GOOD_LCP_MS = 2500;
const GOOD_CLS = 0.1;

interface Vitals {
  lcp: number;
  cls: number;
}

/** Measures the page in its current state: call it once the content the user came for has shown. */
export async function expectGoodWebVitals(page: Page): Promise<void> {
  // Evaluated rather than added as a script tag, which the Content Security Policy of the page would block
  await page.evaluate(await webVitalsScript());
  const vitals = await page.evaluate(measure);

  expect(vitals.lcp, 'The LCP of the page is over 2.5 s').toBeLessThanOrEqual(GOOD_LCP_MS);
  expect(vitals.cls, 'The CLS of the page is over 0.1').toBeLessThanOrEqual(GOOD_CLS);
}

async function webVitalsScript(): Promise<string> {
  // The package exports no path to its browser build, which sits next to its module
  const script = new URL('web-vitals.iife.js', import.meta.resolve('web-vitals'));

  return `${await readFile(fileURLToPath(script), 'utf8')}; globalThis.webVitals = webVitals;`;
}

async function measure(): Promise<Vitals> {
  const { onCLS, onLCP } = (globalThis as unknown as { webVitals: typeof WebVitals }).webVitals;
  const vitals: Partial<Vitals> = {};
  const { promise: reported, resolve: report } = Promise.withResolvers<undefined>();

  function reportOnceBoth(): void {
    if (vitals.lcp !== undefined && vitals.cls !== undefined) {
      report(undefined);
    }
  }

  // The first report of each holds every entry the page buffered since it loaded
  onLCP(({ value }) => {
    vitals.lcp = value;
    reportOnceBoth();
  }, { reportAllChanges: true });
  onCLS(({ value }) => {
    vitals.cls = value;
    reportOnceBoth();
  }, { reportAllChanges: true });
  await reported;

  // As if the user left the page: web-vitals then takes the layout shifts its observer has not delivered yet
  Object.defineProperty(document, 'visibilityState', { value: 'hidden', configurable: true });
  document.dispatchEvent(new Event('visibilitychange'));
  Reflect.deleteProperty(document, 'visibilityState');

  return { lcp: vitals.lcp ?? Number.NaN, cls: vitals.cls ?? Number.NaN };
}
