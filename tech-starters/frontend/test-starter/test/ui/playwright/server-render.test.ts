import { expect, test } from '@playwright/test';
import { http, HttpResponse } from 'msw';
import { startFakeBackend } from '../../../src/fake-backend/index.ts';
import type { FakeBackend } from '../../../src/fake-backend/index.ts';
import { expectSameServerRender } from '../../../src/playwright/index.ts';

let server: FakeBackend | undefined;

test.afterEach(async () => {
  await server?.close();
});

test('a page rendered the same for two requests passes', async ({ request }) => {
  // Given
  server = await startFakeBackend(http.get('/cellars', () => HttpResponse.html('<h1>Cellars</h1>')));

  // When & Then
  await expectSameServerRender(request, `${server.url}/cellars`);
});

test('a page that keeps state from the previous request fails', async ({ request }) => {
  // Given
  let visits = 0;
  server = await startFakeBackend(http.get('/cellars', () => {
    visits += 1;

    return HttpResponse.html(`<h1>Cellars</h1><p>${String(visits)} visits</p>`);
  }));

  // When & Then
  await expect(expectSameServerRender(request, `${server.url}/cellars`)).rejects.toThrow('The second render differs from the first');
});

test('the nonce of each response is not a difference', async ({ request }) => {
  // Given
  let nonce = 0;
  server = await startFakeBackend(http.get('/cellars', () => {
    nonce += 1;

    return HttpResponse.html(`<script nonce="n${String(nonce)}">start()</script><h1>Cellars</h1>`);
  }));

  // When & Then
  await expectSameServerRender(request, `${server.url}/cellars`);
});
