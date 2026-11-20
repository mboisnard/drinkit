import { expect, test } from 'vitest';
import { createClientConfig } from '../../app/client-config.ts';

test('a call without the options of useApi() fails at once rather than leave without the cookies of the user', () => {
  // Given
  const config = createClientConfig({ baseUrl: 'http://127.0.0.1:8080/drinkit' });

  // When & Then
  expect(() => config.onRequest()).toThrow('Pass useApi() of @drinkit/api-client-starter/use-api to each call of the API');
});
