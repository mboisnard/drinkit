import { expect, test } from 'vitest';
import { backendUrlOf } from '../../../shared/api-path.ts';

const BACKEND = 'http://127.0.0.1:8080';
const API = '/drinkit';

test('a call of the API reaches the same path on the backend, with its query', () => {
  // When
  const url = backendUrlOf(BACKEND, API, '/drinkit/api/cellars?page=2');

  // Then
  expect(url?.href).toBe('http://127.0.0.1:8080/drinkit/api/cellars?page=2');
});

test.each(['/drinkit/../actuator/env', '/drinkit/%2e%2e/actuator', '/drinkit/%2E%2E/%2e%2e/admin'])(
  'a path that climbs out of the API, %s, reaches nothing on the backend',
  (path) => {
    // When
    const url = backendUrlOf(BACKEND, API, path);

    // Then
    expect(url).toBeUndefined();
  },
);

test('the API of another app reaches its own path on its backend', () => {
  // When
  const url = backendUrlOf('http://127.0.0.1:8090', '/cellar-book', '/cellar-book/api/bottles');

  // Then
  expect(url?.href).toBe('http://127.0.0.1:8090/cellar-book/api/bottles');
});

test('a path outside the API of the app reaches nothing on the backend', () => {
  // When
  const url = backendUrlOf(BACKEND, '/cellar-book', '/drinkit/api/cellars');

  // Then
  expect(url).toBeUndefined();
});
