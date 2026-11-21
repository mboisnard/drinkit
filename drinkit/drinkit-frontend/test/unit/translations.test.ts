import { fileURLToPath } from 'node:url';
import { test } from 'vitest';
import { expectLocaleParity, expectValidMessages } from '@drinkit/i18n-starter/testing';

const catalog = fileURLToPath(new URL('../../../../translations', import.meta.url));

test('every locale of the catalog holds the keys of en.json, with the same values', () => {
  // When & Then
  expectLocaleParity(catalog);
});

test('every message of the catalog reads the same in the frontend and in the backend', () => {
  // When & Then
  expectValidMessages(catalog);
});
