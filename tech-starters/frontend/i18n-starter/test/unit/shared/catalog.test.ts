import { expect, test } from 'vitest';
import { messagesOf, prefixesWithoutKeys } from '../../../shared/catalog.ts';

const catalog = {
  'auth.forbidden': 'You cannot open this page.',
  'cellars.empty': 'No cellar yet.',
  'cellarsArchive.empty': 'No archived cellar.',
};

test('an app loads the messages of the prefixes it and its layers declare, and no other', () => {
  // When & Then
  expect(messagesOf(catalog, ['auth', 'cellars'])).toEqual({
    'auth.forbidden': 'You cannot open this page.',
    'cellars.empty': 'No cellar yet.',
  });
});

test('a declared prefix that no key starts with is reported, as a typo would be', () => {
  // When & Then
  expect(prefixesWithoutKeys(catalog, ['cellars', 'celars'])).toEqual(['celars']);
});
