/**
 * The defaults of the generated client, which imports this file. Its client is shared by every request of the
 * server, so it holds no cookie: each call takes those of its own request through `useApi()`.
 */
export function createClientConfig<T extends object>(config: T): T & { onRequest: () => never } {
  return {
    ...config,
    onRequest: () => {
      throw new Error('Pass useApi() of @drinkit/api-client-starter/use-api to each call of the API');
    },
  };
}
