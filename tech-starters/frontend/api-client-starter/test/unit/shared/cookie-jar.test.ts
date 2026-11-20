import { expect, test } from 'vitest';
import { CookieJar } from '../../../shared/cookie-jar.ts';

test('the first call of a render carries the cookies of the page', () => {
  // When
  const jar = new CookieJar('remember-me=old; visitor=v1');

  // Then
  expect(jar.cookieHeader()).toBe('remember-me=old; visitor=v1');
});

test('a cookie the backend sets goes with the next call of the same render', () => {
  // Given
  const jar = new CookieJar('remember-me=old');

  // When
  jar.receive(['remember-me=new; Path=/; HttpOnly', 'SESSION=s1; Path=/; HttpOnly']);

  // Then
  expect(jar.cookieHeader()).toBe('remember-me=new; SESSION=s1');
});

test('the page sends the browser the last value the backend set for each cookie', () => {
  // Given
  const jar = new CookieJar('');

  // When
  jar.receive(['visitor=v1; Path=/']);
  jar.receive(['visitor=v2; Path=/']);

  // Then
  expect(jar.setCookies([])).toStrictEqual(['visitor=v2; Path=/']);
});

test('a cookie the page already sets for another reason is kept, once', () => {
  // Given
  const jar = new CookieJar('');
  jar.receive(['visitor=v2; Path=/']);

  // When
  const setCookies = jar.setCookies(['i18n=fr; Path=/', 'visitor=v1; Path=/']);

  // Then
  expect(setCookies).toStrictEqual(['i18n=fr; Path=/', 'visitor=v2; Path=/']);
});

test('the calls of a render carry only the cookies of the API, with or without their __Host- prefix', () => {
  // When
  const jar = new CookieJar('_ga=tracker; __Host-SESSION=s1; remember-me=r1');

  // Then
  expect(jar.cookieHeader()).toBe('__Host-SESSION=s1; remember-me=r1');
});

const EXPIRING = [
  'remember-me=; Max-Age=0; Path=/',
  'remember-me=; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/',
];

test.each(EXPIRING)('a cookie the backend expires leaves the next calls of the render: %s', (expiring) => {
  // Given
  const jar = new CookieJar('SESSION=s1; remember-me=r1');

  // When
  jar.receive([expiring]);

  // Then
  expect(jar.cookieHeader()).toBe('SESSION=s1');
});

test.each(EXPIRING)('the browser is told of a cookie the backend expires: %s', (expiring) => {
  // Given
  const jar = new CookieJar('SESSION=s1; remember-me=r1');

  // When
  jar.receive([expiring]);

  // Then
  expect(jar.setCookies([])).toStrictEqual([expiring]);
});
