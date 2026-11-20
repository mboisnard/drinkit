import { useNuxtApp } from '#app';
import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { getHeader, setResponseHeader, setResponseStatus } from 'h3';
import type { EventHandler } from 'h3';
import { ofetch } from 'ofetch';
import { afterEach, expect, test } from 'vitest';
import type { ProblemDetail } from '@drinkit/nuxt-starter/problem-detail';
import { ProblemError } from '../../app/problem-error.ts';
import { useApi } from '../../app/use-api.ts';
import type { ApiOptions } from '../../app/use-api.ts';

afterEach(() => {
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

function echoCsrfToken(path: string): void {
  registerEndpoint(path, {
    method: 'POST',
    handler: (event) => ({ token: getHeader(event, 'x-xsrf-token') ?? null }),
  });
}

test('a write carries the CSRF token of its cookie, which the backend checks', async () => {
  // Given
  echoCsrfToken('/drinkit/api/cellars');
  document.cookie = 'XSRF-TOKEN=dev-token';

  // When
  const answer = await call(useApi(), '/api/cellars', 'POST');

  // Then
  expect(answer).toStrictEqual({ token: 'dev-token' });
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

test('a read carries no CSRF token', async () => {
  // Given
  registerEndpoint('/drinkit/api/cellars/read', {
    method: 'GET',
    handler: (event) => ({ token: getHeader(event, 'x-xsrf-token') ?? null }),
  });
  document.cookie = 'XSRF-TOKEN=dev-token';

  // When
  const answer = await call(useApi(), '/api/cellars/read');

  // Then
  expect(answer).toStrictEqual({ token: null });
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
