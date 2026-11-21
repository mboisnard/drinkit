import { expect, test } from 'vitest';
import { messageProblems, parityProblems } from '../../../shared/catalog-checks.ts';

test('locales that hold the same keys with the same values to place are at parity', () => {
  // Given
  const catalogs = {
    en: { 'cellar.bottles': '{count, plural, one {# bottle} other {# bottles}}' },
    fr: { 'cellar.bottles': '{count, plural, =0 {aucune bouteille} one {# bouteille} other {# bouteilles}}' },
  };

  // When & Then
  expect(parityProblems(catalogs)).toEqual([]);
});

test('a key present in one locale only breaks the parity', () => {
  // Given
  const catalogs = { en: { 'cellar.empty': 'No cellar yet.', 'cellar.title': 'My cellars' }, fr: { 'cellar.title': 'Mes caves' } };

  // When & Then
  expect(parityProblems(catalogs)).toEqual(['fr.json misses cellar.empty']);
});

test('a translation that places another value than the source breaks the parity', () => {
  // Given
  const catalogs = { en: { 'cellar.welcome': 'Welcome to {cellarName}' }, fr: { 'cellar.welcome': 'Bienvenue dans {name}' } };

  // When & Then
  expect(parityProblems(catalogs)).toEqual(['fr.json places name in cellar.welcome, where en.json places cellarName']);
});

test('a message of plain text, values, numbers, plurals and selects is valid', () => {
  // Given
  const messages = {
    'cellar.bottles': '{count, plural, =0 {aucune bouteille} one {# bouteille} other {# bouteilles}}',
    'cellar.rank': '{rank, selectordinal, one {#st} two {#nd} few {#rd} other {#th}}',
    'cellar.share': '{share, number, percent} of {total, number, integer} in {kind, select, wine {wine} other {spirit}}',
    'cellar.title': 'L’inventaire de {cellarName}',
  };

  // When & Then
  expect(messageProblems(messages)).toEqual([]);
});

test('a message that is not ICU is invalid', () => {
  // When & Then
  expect(messageProblems({ 'cellar.welcome': 'Welcome to {cellarName' })).toEqual([
    'cellar.welcome is not an ICU message: EXPECT_ARGUMENT_CLOSING_BRACE',
  ]);
});

test('a plural without its other branch is invalid, since a language may have no form for a count', () => {
  // When & Then
  expect(messageProblems({ 'cellar.bottles': '{count, plural, one {# bottle}}' })).toEqual([
    'cellar.bottles is not an ICU message: MISSING_OTHER_CLAUSE',
  ]);
});

test('an ASCII apostrophe is refused, since ICU4J and formatjs read it as a quote in different places', () => {
  // When & Then
  expect(messageProblems({ 'cellar.title': 'L\'inventaire de {cellarName}' })).toEqual([
    'cellar.title holds an ASCII apostrophe: write ’',
  ]);
});

test('a message that only one of ICU4J and formatjs reads the same is refused', () => {
  // Given
  const messages = {
    'cellar.amount': '{amount, number, ::currency/EUR}',
    'cellar.date': '{openedAt, date, short}',
    'cellar.link': 'See <b>{cellarName}</b>',
    'cellar.offset': '{count, plural, offset:1 one {# other} other {# others}}',
  };

  // When & Then
  expect(messageProblems(messages)).toEqual([
    'cellar.amount formats a number with a skeleton or a pattern: format it with n()',
    'cellar.date formats a date: format it with d()',
    'cellar.link holds a tag',
    'cellar.offset uses a plural offset',
  ]);
});
