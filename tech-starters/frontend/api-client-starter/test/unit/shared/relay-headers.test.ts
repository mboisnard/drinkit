import { expect, test } from 'vitest';
import { relayHeaders } from '../../../shared/relay-headers.ts';

const CLIENT = { ip: '203.0.113.7', protocol: 'https' } as const;

test('a browser request reaches the backend with only the headers the API reads', () => {
  // Given
  const incoming = new Headers({
    'accept': 'application/json',
    'cookie': 'SESSION=1',
    'x-xsrf-token': 'token',
    'idempotency-key': 'key',
    'host': 'drinkit.example',
    'connection': 'keep-alive',
    'authorization': 'Basic leaked',
  });

  // When
  const headers = relayHeaders(incoming, CLIENT, 'browser');

  // Then
  expect(headers.get('accept')).toBe('application/json');
  expect(headers.get('cookie')).toBe('SESSION=1');
  expect(headers.get('x-xsrf-token')).toBe('token');
  expect(headers.get('idempotency-key')).toBe('key');
  expect(headers.has('host')).toBe(false);
  expect(headers.has('connection')).toBe(false);
  expect(headers.has('authorization')).toBe(false);
});

test('a browser request carries only the cookies of the API to the backend', () => {
  // Given
  const incoming = new Headers({ cookie: '_ga=tracker; SESSION=1' });

  // When
  const headers = relayHeaders(incoming, CLIENT, 'browser');

  // Then
  expect(headers.get('cookie')).toBe('SESSION=1');
});

test('a browser request without a cookie of the API carries no cookie', () => {
  // When
  const headers = relayHeaders(new Headers({ cookie: '_ga=tracker' }), CLIENT, 'browser');

  // Then
  expect(headers.has('cookie')).toBe(false);
});

test('the backend sees the address and the scheme of the client, never those the client claims', () => {
  // Given
  const incoming = new Headers({ 'x-forwarded-for': '198.51.100.1', 'x-forwarded-proto': 'http' });

  // When
  const headers = relayHeaders(incoming, CLIENT, 'browser');

  // Then
  expect(headers.get('x-forwarded-for')).toBe('203.0.113.7');
  expect(headers.get('x-forwarded-proto')).toBe('https');
});

test('the fetch metadata of a browser request reach the backend, which refuses a cross-site write', () => {
  // Given
  const incoming = new Headers({ 'sec-fetch-site': 'cross-site', 'sec-fetch-mode': 'cors' });

  // When
  const headers = relayHeaders(incoming, CLIENT, 'browser');

  // Then
  expect(headers.get('sec-fetch-site')).toBe('cross-site');
  expect(headers.get('sec-fetch-mode')).toBe('cors');
});

test('a call of the server render carries the cookies of the page but not its fetch metadata', () => {
  // Given
  const page = new Headers({
    'accept': 'text/html',
    'cookie': 'remember-me=1',
    'accept-language': 'fr',
    'sec-fetch-site': 'cross-site',
  });

  // When
  const headers = relayHeaders(page, CLIENT, 'server-render');

  // Then
  expect(headers.get('cookie')).toBe('remember-me=1');
  expect(headers.get('accept-language')).toBe('fr');
  expect(headers.has('sec-fetch-site')).toBe(false);
  expect(headers.has('accept')).toBe(false);
});
