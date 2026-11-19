import { http, HttpResponse } from 'msw';
import { afterEach, expect, test } from 'vitest';
import { startFakeBackend } from '../../../src/fake-backend/index.ts';
import type { FakeBackend } from '../../../src/fake-backend/index.ts';

let backend: FakeBackend | undefined;

afterEach(async () => {
  await backend?.close();
  backend = undefined;
});

test('a request the handlers know gets their response over HTTP', async () => {
  // Given
  backend = await startFakeBackend(http.get('/cellars/:id', ({ params }) => HttpResponse.json({ id: params.id })));

  // When
  const response = await fetch(`${backend.url}/cellars/42`);

  // Then
  expect(await response.json()).toStrictEqual({ id: '42' });
});

test('a request no handler knows fails with its method and path, so a missing handler never goes unnoticed', async () => {
  // Given
  backend = await startFakeBackend();

  // When
  const response = await fetch(`${backend.url}/cellars?page=2`, { method: 'POST' });

  // Then
  expect(response.status).toBe(501);
  expect(await response.text()).toBe('No handler of the fake backend for POST /cellars?page=2');
});

test('the body of a request reaches the handler', async () => {
  // Given
  backend = await startFakeBackend(http.post('/cellars', async ({ request }) => HttpResponse.json(await request.json())));

  // When
  const response = await fetch(`${backend.url}/cellars`, { method: 'POST', body: JSON.stringify({ name: 'Home' }) });

  // Then
  expect(await response.json()).toStrictEqual({ name: 'Home' });
});

test('a closed fake backend no longer answers', async () => {
  // Given
  const closed = await startFakeBackend();
  await closed.close();

  // When & Then
  await expect(fetch(closed.url)).rejects.toThrow('fetch failed');
});

test('a fake backend closes even while a request still waits for its answer', async () => {
  // Given
  const { promise: received, resolve: receive } = Promise.withResolvers<undefined>();
  const stuck = await startFakeBackend(http.get('/cellars', () => {
    receive(undefined);

    return new Promise<never>(() => undefined);
  }));
  const pending = fetch(`${stuck.url}/cellars`).catch(() => undefined);
  await received;

  // When
  await stuck.close();

  // Then
  await expect(pending).resolves.toBeUndefined();
}, 2000);
