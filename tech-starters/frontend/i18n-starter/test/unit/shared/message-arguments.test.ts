import { expect, test } from 'vitest';
import { argumentsOf } from '../../../shared/message-arguments.ts';

test('a message with no argument places no value', () => {
  // When & Then
  expect(argumentsOf('No cellar yet.')).toEqual({});
});

test('a plain argument takes a text or a number, a counted one a number, a chosen one a text', () => {
  // When & Then
  expect(argumentsOf('{name} keeps {count, plural, one {# bottle} other {# bottles}} for {owner, select, other {them}}'))
    .toEqual({ name: 'string | number', count: 'number', owner: 'string' });
});

test('an argument a branch places again keeps the type of the one that chooses the branch', () => {
  // When & Then
  expect(argumentsOf('{count, plural, one {one bottle} other {{count} bottles in {rank, selectordinal, other {#th}}}}'))
    .toEqual({ count: 'number', rank: 'number' });
});
