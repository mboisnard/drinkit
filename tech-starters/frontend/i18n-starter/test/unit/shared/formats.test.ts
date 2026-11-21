import { afterEach, expect, test } from 'vitest';
import { formatDate, formatNumber } from '../../../shared/formats.ts';

const hostTimeZone = process.env.TZ;

afterEach(() => {
  process.env.TZ = hostTimeZone;
});

test('a date reads the same whatever the time zone of the machine that renders it', () => {
  // Given
  const openedAt = new Date('2026-11-21T23:30:00Z');

  // When
  process.env.TZ = 'America/Los_Angeles';
  const onTheServer = formatDate(openedAt, 'dateTime', { language: 'fr-FR', timeZone: 'Europe/Paris' });
  process.env.TZ = 'Pacific/Kiritimati';
  const inTheBrowser = formatDate(openedAt, 'dateTime', { language: 'fr-FR', timeZone: 'Europe/Paris' });

  // Then
  expect([onTheServer, inTheBrowser]).toEqual(['22 nov. 2026, 00:30', '22 nov. 2026, 00:30']);
});

test('a date follows the conventions of the language of its locale', () => {
  // Given
  const openedAt = new Date('2026-03-04T12:00:00Z');

  // When & Then
  expect(formatDate(openedAt, 'date', { language: 'en-GB', timeZone: 'Europe/Paris' })).toBe('4 Mar 2026');
});

test('an amount shows in the currency it is given, in the conventions of the language', () => {
  // When & Then
  expect(formatNumber(1234.5, 'currency', { language: 'fr-FR', currency: 'EUR' })).toBe('1\u202F234,50\u00A0€');
});

test('a share shows as a percentage', () => {
  // When & Then
  expect(formatNumber(0.25, 'percent', { language: 'en-GB' })).toBe('25%');
});
