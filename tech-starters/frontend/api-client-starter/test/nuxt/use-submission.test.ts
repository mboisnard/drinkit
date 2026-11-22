import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { getHeader, setResponseHeader, setResponseStatus } from 'h3';
import { ofetch } from 'ofetch';
import { afterEach, beforeEach, expect, test } from 'vitest';
import type { ApiOptions } from '@drinkit/api-client-starter/use-api';
import { useSubmission } from '@drinkit/api-client-starter/use-submission';

beforeEach(() => {
  document.cookie = 'XSRF-TOKEN=dev-token';
});

afterEach(() => {
  document.cookie = 'XSRF-TOKEN=; max-age=0';
});

// What the generated client does with the options it is given
async function deleteThing(api: ApiOptions, path: string, body?: object): Promise<unknown> {
  return ofetch(`${api.baseUrl}${path}`, {
    method: 'DELETE',
    body,
    onRequest: api.onRequest,
    onResponse: api.onResponse,
  });
}

/** A backend that answers each call with the next status, then 204, and remembers the key of each call. */
function backendAnswering(path: string, ...statuses: number[]): Array<string | undefined> {
  const keys: Array<string | undefined> = [];
  registerEndpoint(`/drinkit${path}`, {
    method: 'DELETE',
    handler: (event) => {
      keys.push(getHeader(event, 'idempotency-key'));
      const status = statuses.shift() ?? 204;
      setResponseStatus(event, status);
      if (status === 409) {
        setResponseHeader(event, 'retry-after', 0);
      }

      return status >= 400 ? { status, title: 'Refused', detail: 'Refused by the backend' } : null;
    },
  });

  return keys;
}

test.each([
  ['a server error', 500],
  ['an answer lost behind the relay', 502],
  ['too many requests', 429],
])('a submission whose outcome is unknown, after %s, keeps its key for the next one', async (_, status) => {
  // Given
  const keys = backendAnswering(`/unknown/${String(status)}`, status);
  const { submit } = useSubmission();
  await submit((api) => deleteThing(api, `/unknown/${String(status)}`));

  // When
  await submit((api) => deleteThing(api, `/unknown/${String(status)}`));

  // Then
  expect(keys).toHaveLength(2);
  expect(keys[0]).toBeDefined();
  expect(keys[1]).toBe(keys[0]);
});

test.each([
  ['after a success', 204, '/known/success', '/known/success'],
  ['after a refusal', 404, '/known/refusal', '/known/refusal'],
  ['for another request', 500, '/unsettled', '/another'],
])('%s, a submission gets a new key', async (_, status, firstPath, nextPath) => {
  // Given
  const firstKeys = backendAnswering(firstPath, status);
  const nextKeys = nextPath === firstPath ? firstKeys : backendAnswering(nextPath);
  const { submit } = useSubmission();
  await submit((api) => deleteThing(api, firstPath));

  // When
  await submit((api) => deleteThing(api, nextPath));

  // Then
  expect(nextKeys.at(-1)).toBeDefined();
  expect(nextKeys.at(-1)).not.toBe(firstKeys[0]);
});

test('another body sent to the same URL gets a new key, while the outcome of the first is still unknown', async () => {
  // Given
  const keys = backendAnswering('/same-url', 500);
  const { submit } = useSubmission();
  await submit((api) => deleteThing(api, '/same-url', { reason: 'moved' }));

  // When
  await submit((api) => deleteThing(api, '/same-url', { reason: 'sold' }));

  // Then
  expect(keys).toHaveLength(2);
  expect(keys[1]).not.toBe(keys[0]);
});

test('a submission still in progress on the backend is sent again with its key, and shows no problem', async () => {
  // Given
  const keys = backendAnswering('/in-progress', 409, 409);
  const { problem, submit } = useSubmission();

  // When
  await submit((api) => deleteThing(api, '/in-progress'));

  // Then
  expect(keys).toHaveLength(3);
  expect(new Set(keys).size).toBe(1);
  expect(problem.value).toBeUndefined();
});

test.each([
  ['the detail of the backend, by default', '/refused/default', undefined, 'Refused by the backend'],
  ['the message the form gives', '/refused/own', () => 'Already deleted', 'Already deleted'],
  ['the detail of the backend, when the form gives no message', '/refused/none', () => undefined, 'Refused by the backend'],
])('a refusal shows %s', async (_, path, messageOf, shown) => {
  // Given
  backendAnswering(path, 404);
  const { problem, submit } = useSubmission(messageOf);

  // When
  await submit((api) => deleteThing(api, path));

  // Then
  expect(problem.value).toBe(shown);
});
