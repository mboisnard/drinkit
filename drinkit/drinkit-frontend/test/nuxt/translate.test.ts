import { expect, test } from 'vitest';
import { useTranslate } from '@drinkit/i18n-starter/translate';

test('the catalog translates its keys, and the type check refuses a key it lacks', () => {
  // Given
  const { t } = useTranslate();

  // When
  // @ts-expect-error -- the catalog has no such key, which would show as itself
  const unknown = t('cellars.nope');

  // Then
  expect({ known: t('cellars.title'), unknown }).toStrictEqual({ known: 'My cellars', unknown: 'cellars.nope' });
});

test('the type check refuses a message without the values it places, which would fail as it renders', () => {
  // Given
  const { t } = useTranslate();

  // When & Then
  // @ts-expect-error -- the greeting places a first name
  expect(() => t('home.greeting')).toThrow();
  // @ts-expect-error -- the greeting places no name but the first name
  expect(() => t('home.greeting', { name: 'Ann' })).toThrow();
  expect(t('home.greeting', { firstname: 'Ann' })).toBe('Welcome back, Ann');
});
