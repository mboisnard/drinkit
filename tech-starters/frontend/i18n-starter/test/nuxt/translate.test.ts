import { useNuxtApp } from '#app';
import { afterEach, expect, test } from 'vitest';
import { useTranslate } from '../../app/translate.ts';
import { useFixtureTranslate } from './fixture-translate.ts';

afterEach(async () => {
  await useNuxtApp().$i18n.setLocale('en');
});

test('a message counts in the plural rules of English', () => {
  // Given
  const t = useFixtureTranslate();

  // When & Then
  expect([t('cellar.bottles', { count: 1 }), t('cellar.bottles', { count: 2 })]).toEqual(['1 bottle', '2 bottles']);
});

test('a message counts in the plural rules of French, where 1 and 0 are singular', async () => {
  // Given
  await useNuxtApp().$i18n.setLocale('fr');
  const t = useFixtureTranslate();

  // When & Then
  expect([t('cellar.bottles', { count: 0 }), t('cellar.bottles', { count: 1 })]).toEqual(['aucune bouteille', '1 bouteille']);
});

test('a message places a named value', () => {
  // Given
  const t = useFixtureTranslate();

  // When & Then
  expect(t('cellar.welcome', { cellarName: 'La Cave' })).toBe('Welcome to La Cave');
});

test('the keys of a prefix no layer declares are not loaded', () => {
  // Given
  const t = useFixtureTranslate();

  // When & Then
  expect(t('hidden.secret')).toBe('hidden.secret');
});

test('a date follows the language of the locale, in the time zone the caller gives', async () => {
  // Given
  await useNuxtApp().$i18n.setLocale('fr');
  const { d } = useTranslate();

  // When & Then
  expect(d(new Date('2026-11-21T23:30:00Z'), 'dateLong', 'Europe/Paris')).toBe('22 novembre 2026');
});

test('an amount shows in the currency the caller gives, which the type check requires', () => {
  // Given
  const { n } = useTranslate();

  // When & Then
  expect(n(12, 'currency', 'EUR')).toBe('€12.00');
  // @ts-expect-error -- an amount without its currency
  expect(() => n(12, 'currency')).toThrow(TypeError);
});

test('a key built at run time, such as one per status, is known only when the catalog holds it', () => {
  // Given
  const { exists } = useTranslate();

  // When & Then
  expect([exists('cellar.welcome'), exists('cellar.goodbye')]).toEqual([true, false]);
});
