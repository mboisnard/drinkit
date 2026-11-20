import { useAsyncData } from '#app';
import type { AsyncData, NuxtError } from '#app';
import { useApi } from '@drinkit/api-client-starter/use-api';
import { findCellars } from '#api';
import type { CellarResponse } from '#api';

/** The cellars of the signed-in user, read during the server render of the page. */
export function useCellars(): AsyncData<CellarResponse[] | undefined, NuxtError | undefined> {
  const api = useApi();

  return useAsyncData('cellars', async () => (await findCellars(api)).data.cellars);
}
