import { expect, test } from 'vitest';
import { accessTo } from '../../../shared/access.ts';

const registering = { roles: ['ROLE_REGISTRATION_IN_PROGRESS'] };
const member = { roles: ['ROLE_USER'] };
const noRedirect = {};

test('a public page opens to a visitor', () => {
  // When
  const access = accessTo('public', null, noRedirect);

  // Then
  expect(access).toStrictEqual({ kind: 'open' });
});

test('a visitor asks to sign in on a page for signed-in users', () => {
  // When
  const access = accessTo('signed-in', null, noRedirect);

  // Then
  expect(access).toStrictEqual({ kind: 'sign-in' });
});

test('a page without auth is for signed-in users', () => {
  // When
  const access = accessTo(undefined, null, noRedirect);

  // Then
  expect(access).toStrictEqual({ kind: 'sign-in' });
});

test('a page for signed-in users opens to any signed-in user', () => {
  // When
  const access = accessTo('signed-in', registering, noRedirect);

  // Then
  expect(access).toStrictEqual({ kind: 'open' });
});

test('a page for a role opens to a user holding it', () => {
  // When
  const access = accessTo('ROLE_USER', member, noRedirect);

  // Then
  expect(access).toStrictEqual({ kind: 'open' });
});

test('a page for a role is forbidden to a user without it', () => {
  // When
  const access = accessTo('ROLE_ADMIN', member, noRedirect);

  // Then
  expect(access).toStrictEqual({ kind: 'forbidden' });
});

test('a user without the role goes to the page the app gives one of their roles', () => {
  // When
  const access = accessTo('ROLE_USER', registering, { ROLE_REGISTRATION_IN_PROGRESS: '/registration' });

  // Then
  expect(access).toStrictEqual({ kind: 'redirect', to: '/registration' });
});
