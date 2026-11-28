import { useAsyncData } from '#app';
import type { AsyncData, NuxtError } from '#app';
import { useApi } from '@drinkit/api-client-starter/use-api';
import { listPasskeys } from '#api';
import type { Passkey } from '#api';

/** The passkeys of the signed-in user, oldest first, read during the server render of the page. */
export function usePasskeyList(): AsyncData<Passkey[] | undefined, NuxtError | undefined> {
  const api = useApi();

  return useAsyncData('passkeys', async () => (await listPasskeys(api)).data);
}
