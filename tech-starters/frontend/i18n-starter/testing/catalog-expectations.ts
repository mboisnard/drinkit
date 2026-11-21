import { readdirSync, readFileSync } from 'node:fs';
import { basename, join } from 'node:path';
import { expect } from 'vitest';
import { messageProblems, parityProblems } from '../shared/catalog-checks.ts';
import type { Messages } from '../shared/catalog.ts';

/** Fails when a locale of the catalog folder misses a key of `en.json`, holds an extra one, or places other values. */
export function expectLocaleParity(folder: string): void {
  const problems = parityProblems(catalogsOf(folder));

  expect(problems, problems.join('\n')).toEqual([]);
}

/** Fails when a message of the catalog folder is not ICU, or would not read the same in formatjs and in ICU4J. */
export function expectValidMessages(folder: string): void {
  const problems = Object.entries(catalogsOf(folder)).flatMap(([locale, messages]) =>
    messageProblems(messages).map((problem) => `${locale}.json: ${problem}`),
  );

  expect(problems, problems.join('\n')).toEqual([]);
}

function catalogsOf(folder: string): Record<string, Messages> {
  const files = readdirSync(folder).filter((file) => file.endsWith('.json'));

  return Object.fromEntries(
    files.map((file) => [basename(file, '.json'), JSON.parse(readFileSync(join(folder, file), 'utf8')) as Messages]),
  );
}
