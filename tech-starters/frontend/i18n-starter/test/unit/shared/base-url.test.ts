import { expect, test } from 'vitest';
import { baseUrlProblem } from '../../../shared/base-url.ts';

test('the origin of a deployment is a base URL its links to the other languages can name', () => {
  // When & Then
  expect(baseUrlProblem('https://drinkit.app')).toBeUndefined();
});

test.each(['', 'http://localhost:3000', 'http://127.0.0.1:3000', 'https://drinkit.localhost', 'drinkit.app'])(
  'a base URL a search engine cannot reach is refused: %j',
  (baseUrl) => {
    // When & Then
    expect(baseUrlProblem(baseUrl)).toMatch(/^i18n\.baseUrl /);
  },
);
