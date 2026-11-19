import AxeBuilder from '@axe-core/playwright';
import { expect } from '@playwright/test';
import type { Page } from '@playwright/test';

// WCAG 2.2 AA, and the one rule outside it that a page with no level one heading breaks
const WCAG_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const HEADING_RULE = 'page-has-heading-one';

export async function expectAccessible(page: Page): Promise<void> {
  // One run at a time: axe refuses a second run on a page it is still analysing
  const wcag = await new AxeBuilder({ page }).withTags(WCAG_TAGS).analyze();
  const heading = await new AxeBuilder({ page }).withRules(HEADING_RULE).analyze();
  const broken = [...wcag.violations, ...heading.violations].map((violation) => `${violation.id}: ${violation.help}`);

  expect(broken, 'The page breaks accessibility rules').toStrictEqual([]);
}
