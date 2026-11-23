import { request as rawRequest } from 'node:http';
import { buffer } from 'node:stream/consumers';
import { http, HttpResponse } from 'msw';
import { expect, test } from '@drinkit/test-starter/playwright';
import { CURRENT_USER_PATH } from './cellar-backend.ts';

test('the cookies the backend sets during a server render reach the browser with the page', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.get(CURRENT_USER_PATH, () => HttpResponse.json(
    { roles: ['ROLE_USER'] },
    { headers: { 'set-cookie': 'SESSION=s1; Path=/; HttpOnly' } },
  )));

  // When
  const page = await request.get('/', { headers: { cookie: 'remember-me=r1' } });

  // Then
  expect(page.headersArray().filter(({ name }) => name.toLowerCase() === 'set-cookie').map(({ value }) => value))
    .toContain('SESSION=s1; Path=/; HttpOnly');
});

test('two server renders at once each call the backend with the cookies of their own user', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.get(CURRENT_USER_PATH, ({ cookies }) => HttpResponse.json(
    { firstname: cookies.SESSION, roles: ['ROLE_USER'] },
  )));

  // When
  const [camille, dominique] = await Promise.all([
    request.get('/', { headers: { cookie: 'SESSION=Camille' } }),
    request.get('/', { headers: { cookie: 'SESSION=Dominique' } }),
  ]);

  // Then
  expect(await camille.text()).toContain('Welcome back, Camille');
  expect(await dominique.text()).toContain('Welcome back, Dominique');
});

test('a call of the browser reaches the backend through the relay, with its body and its CSRF token', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.post('/drinkit/api/cellars', async ({ request: call }) => HttpResponse.json({
    body: await call.json() as unknown,
    token: call.headers.get('x-xsrf-token'),
  }, { status: 201 })));

  // When
  const answer = await request.post('/drinkit/api/cellars', {
    data: { name: 'Home' },
    headers: { 'x-xsrf-token': 'token' },
  });

  // Then
  expect(answer.status()).toBe(201);
  expect(await answer.json()).toStrictEqual({ body: { name: 'Home' }, token: 'token' });
});

test('the backend sees the address of the browser, not one the browser claims', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.get('/drinkit/api/whoami', ({ request: call }) => HttpResponse.json({
    address: call.headers.get('x-forwarded-for'),
  })));

  // When
  const answer = await request.get('/drinkit/api/whoami', { headers: { 'x-forwarded-for': '198.51.100.1' } });

  // Then
  expect(await answer.json()).toStrictEqual({ address: '127.0.0.1' });
});

test('the backend sees the scheme the browser used, not one the browser claims', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.get('/drinkit/api/whoami', ({ request: call }) => HttpResponse.json({
    scheme: call.headers.get('x-forwarded-proto'),
  })));

  // When
  const answer = await request.get('/drinkit/api/whoami', { headers: { 'x-forwarded-proto': 'https' } });

  // Then
  expect(await answer.json()).toStrictEqual({ scheme: 'http' });
});

test('a body over 2 MB reaches the backend through the relay, which alone decides its size', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.post('/drinkit/api/cellars', async ({ request: call }) => HttpResponse.json({
    received: (await call.arrayBuffer()).byteLength,
  }, { status: 201 })));
  const threeMegabytes = JSON.stringify({ name: 'x'.repeat(3_000_000) });

  // When
  const answer = await request.post('/drinkit/api/cellars', {
    data: threeMegabytes,
    headers: { 'content-type': 'application/json', 'x-xsrf-token': 'token' },
  });

  // Then
  expect(answer.status()).toBe(201);
  expect(await answer.json()).toStrictEqual({ received: threeMegabytes.length });
});

test('a call the backend cannot answer gets a 502 problem from the relay', async ({ request, fakeBackend }) => {
  // Given
  fakeBackend.use(http.get('/drinkit/api/cellars', () => HttpResponse.error()));

  // When
  const answer = await request.get('/drinkit/api/cellars');

  // Then
  expect(answer.status()).toBe(502);
  expect(answer.headers()['content-type']).toBe('application/problem+json');
  expect(await answer.json()).toStrictEqual({ status: 502, title: 'Bad Gateway' });
});

test('a path that climbs out of the API gets a 404 problem from the relay', async ({ baseURL }) => {
  // When
  const answer = await getAsIs(baseURL ?? '', '/drinkit/api/%2e%2e/%2e%2e/actuator/env');

  // Then
  expect(answer.status).toBe(404);
  expect(answer.contentType).toBe('application/problem+json');
  expect(JSON.parse(answer.body)).toStrictEqual({ status: 404, title: 'Not Found' });
});

// A browser, as the request of Playwright, resolves the dot segments of a path before sending it
interface RawAnswer { status?: number; contentType?: string; body: string }

async function getAsIs(baseURL: string, path: string): Promise<RawAnswer> {
  const { hostname, port } = new URL(baseURL);
  const response = await new Promise<import('node:http').IncomingMessage>((resolve, reject) => {
    rawRequest({ hostname, port, path }, resolve).on('error', reject).end();
  });

  return {
    status: response.statusCode,
    contentType: response.headers['content-type'],
    body: (await buffer(response)).toString(),
  };
}
