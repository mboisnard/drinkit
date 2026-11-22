import { fileURLToPath } from 'node:url';
import { expect, test } from 'vitest';
import { expectLocaleParity, expectValidMessages } from '@drinkit/i18n-starter/testing';

const balanced = fileURLToPath(new URL('../../fixtures/translations', import.meta.url));
const unbalanced = fileURLToPath(new URL('../../fixtures/unbalanced', import.meta.url));

test('a catalog whose locales hold the same keys passes the parity check', () => {
  // When & Then
  expectLocaleParity(balanced);
});

test('a catalog with a key in one locale only fails the parity check', () => {
  // When & Then
  expect(() => {
    expectLocaleParity(unbalanced);
  }).toThrow('fr.json misses cellar.empty');
});

test('a catalog of portable ICU messages passes the message check', () => {
  // When & Then
  expectValidMessages(balanced);
});

test('a catalog with an unportable message fails the message check, naming its locale', () => {
  // When & Then
  expect(() => {
    expectValidMessages(unbalanced);
  }).toThrow('fr.json: cellar.title holds an ASCII apostrophe');
});
