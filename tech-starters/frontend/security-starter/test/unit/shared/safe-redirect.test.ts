import { expect, test } from 'vitest';
import { safeRedirect } from '../../../shared/safe-redirect.ts';

test('a path of the app is kept, with its query and fragment', () => {
  // When
  const redirect = safeRedirect('/cellars?page=2#bordeaux');

  // Then
  expect(redirect).toBe('/cellars?page=2#bordeaux');
});

test.each([
  ['an absolute URL', 'https://evil.example/cellars'],
  ['a protocol-relative URL', '//evil.example/cellars'],
  ['a backslash the browser reads as a slash', '/\\evil.example'],
  ['a tab the browser drops', '/\t/evil.example'],
  ['a dot segment before a double slash', '/..//evil.example'],
  ['a current-directory segment before a double slash', '/.//evil.example'],
  ['a parent segment that lands on a double slash', '/a/..//evil.example'],
  ['a script', 'javascript:alert(1)'],
  ['a relative path', 'cellars'],
])('a return URL to another origin is refused: %s', (_case, value) => {
  // When
  const redirect = safeRedirect(value);

  // Then
  expect(redirect).toBeUndefined();
});

test('a return URL that is not one string is refused', () => {
  // When
  const redirect = safeRedirect(['/cellars', '/registration']);

  // Then
  expect(redirect).toBeUndefined();
});
