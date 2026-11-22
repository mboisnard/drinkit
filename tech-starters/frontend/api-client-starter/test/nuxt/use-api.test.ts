import { useNuxtApp, useRuntimeConfig } from '#app';
import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { getHeader, setResponseHeader, setResponseStatus } from 'h3';
import type { EventHandler } from 'h3';
import { ofetch } from 'ofetch';
import { afterEach, expect, test } from 'vitest';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useApi } from '@drinkit/api-client-starter/use-api';
import type { ApiOptions } from '@drinkit/api-client-starter/use-api';
import type { ProblemDetail } from '@drinkit/nuxt-starter/problem-detail';

const CSRF_URL = '/drinkit/api/auth/csrf';

afterEach(async () => {
  await useNuxtApp().$i18n.setLocale('en');
  document.cookie = '__Host-XSRF-TOKEN=; max-age=0; Secure; Path=/';
  document.cookie = 'XSRF-TOKEN=; max-age=0';
});

// What the generated client does with the options of useApi()
async function call(api: ApiOptions, path: string, method = 'GET'): Promise<unknown> {
  const response = await ofetch.raw(`${api.baseUrl}${path}`, {
    method,
    onRequest: api.onRequest,
    onResponse: api.onResponse,
    ignoreResponseError: true,
  });

  return response._data;
}

// How the backend refuses a call: RFC 9457, the body of Spring's ProblemDetail
function problem(status: number, statusText: string, body: ProblemDetail): EventHandler {
  return (event) => {
    setResponseStatus(event, status, statusText);
    setResponseHeader(event, 'content-type', 'application/problem+json');

    return body;
  };
}

// The relay answers with the cookie, which the browser keeps: the endpoint writes it the way the browser would
function serveCsrfToken(path: string, token: string): { calls: number } {
  const served = { calls: 0 };
  registerEndpoint(path, {
    method: 'GET',
    handler: (event) => {
      served.calls++;
      document.cookie = `XSRF-TOKEN=${token}`;
      setResponseStatus(event, 204);

      return null;
    },
  });

  return served;
}

function echoCsrfToken(path: string): void {
  registerEndpoint(path, {
    method: 'POST',
    handler: (event) => ({ token: getHeader(event, 'x-xsrf-token') ?? null }),
  });
}

test('a write carries the CSRF token of its cookie, which the backend checks, and fetches none', async () => {
  // Given
  const csrf = serveCsrfToken(CSRF_URL, 'fetched-token');
  echoCsrfToken('/drinkit/api/cellars');
  document.cookie = 'XSRF-TOKEN=dev-token';

  // When
  const answer = await call(useApi(), '/api/cellars', 'POST');

  // Then
  expect({ answer, fetches: csrf.calls }).toStrictEqual({ answer: { token: 'dev-token' }, fetches: 0 });
});

test('the token of the __Host- cookie wins over a cookie another site could have planted', async () => {
  // Given
  echoCsrfToken('/drinkit/api/cellars/host');
  document.cookie = 'XSRF-TOKEN=planted';
  document.cookie = '__Host-XSRF-TOKEN=real; Secure; Path=/';

  // When
  const answer = await call(useApi(), '/api/cellars/host', 'POST');

  // Then
  expect(answer).toStrictEqual({ token: 'real' });
});

test('a read neither fetches a CSRF token nor carries one', async () => {
  // Given
  const csrf = serveCsrfToken(CSRF_URL, 'fetched-token');
  registerEndpoint('/drinkit/api/cellars/read', {
    method: 'GET',
    handler: (event) => ({ token: getHeader(event, 'x-xsrf-token') ?? null }),
  });
  document.cookie = 'XSRF-TOKEN=dev-token';

  // When
  const answer = await call(useApi(), '/api/cellars/read');

  // Then
  expect({ answer, fetches: csrf.calls }).toStrictEqual({ answer: { token: null }, fetches: 0 });
});

test('a problem answer raises a ProblemError with the problem the backend sent', async () => {
  // Given
  registerEndpoint('/drinkit/api/cellars/full', problem(409, 'Conflict', { title: 'Cellar full' }));

  // When
  const answer = call(useApi(), '/api/cellars/full');

  // Then
  await expect(answer).rejects.toStrictEqual(
    new ProblemError({ status: 409, statusText: 'Conflict' }, { title: 'Cellar full', status: 409 }),
  );
});

test('a problem answer is told to the drinkit:api:problem hook', async () => {
  // Given
  registerEndpoint('/drinkit/api/cellars/gone', problem(404, 'Not Found', { title: 'No such cellar' }));
  const problems: ProblemDetail[] = [];
  const unhook = useNuxtApp().hook('drinkit:api:problem', (problem) => {
    problems.push(problem);
  });

  // When
  await call(useApi(), '/api/cellars/gone').catch(() => undefined);

  // Then
  unhook();
  expect(problems).toStrictEqual([{ title: 'No such cellar', status: 404 }]);
});

test('a write without a CSRF cookie first fetches the token, then carries it', async () => {
  // Given
  serveCsrfToken(CSRF_URL, 'fetched-token');
  echoCsrfToken('/drinkit/api/auth/code');

  // When
  const answer = await call(useApi(), '/api/auth/code', 'POST');

  // Then
  expect(answer).toStrictEqual({ token: 'fetched-token' });
});

test('without a CSRF path, a write without a cookie leaves without a token', async () => {
  // Given
  const csrf = serveCsrfToken(CSRF_URL, 'fetched-token');
  echoCsrfToken('/drinkit/api/cellars/no-csrf-path');
  const apiClient = useRuntimeConfig().public.apiClient;
  apiClient.csrfPath = '';

  // When
  const answer = await call(useApi(), '/api/cellars/no-csrf-path', 'POST').finally(() => {
    apiClient.csrfPath = '/api/auth/csrf';
  });

  // Then
  expect({ answer, fetches: csrf.calls }).toStrictEqual({ answer: { token: null }, fetches: 0 });
});

test('a call asks the backend for the language of the page, which the texts it sends follow', async () => {
  // Given
  registerEndpoint('/drinkit/api/language', { method: 'GET', handler: (event) => ({ language: getHeader(event, 'accept-language') }) });
  await useNuxtApp().$i18n.setLocale('fr');

  // When
  const answer = await call(useApi(), '/api/language');

  // Then
  expect(answer).toStrictEqual({ language: 'fr-FR' });
});
