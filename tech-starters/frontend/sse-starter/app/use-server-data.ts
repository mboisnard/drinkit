import { useAsyncData } from '#app';
import type { AsyncData, NuxtError } from '#app';
import { onMissedEvents, useServerEvent } from './use-server-event.ts';

export type ServerData<T> = AsyncData<T | undefined, NuxtError | undefined>;

/**
 * Data a page reads through `read`, kept up to date by the events of the backend: each event of `reducers` changes it
 * with no call to the API, and a reconnection after a missed event reads it again, since the backend replays none. It
 * reads in the server render, as `useAsyncData` does, under `key`.
 *
 * Each reducer declares the type of its payload, which nothing checks at runtime.
 */
export function useServerData<T>(
  key: string,
  read: () => Promise<T>,
  reducers: Record<string, (data: T, payload: never) => T>,
): ServerData<T> {
  const page = useAsyncData(key, read);

  Object.entries(reducers).forEach(([name, reducer]) => {
    useServerEvent(name, (payload) => {
      // Before the first read lands, the read itself brings the state
      if (page.data.value !== undefined) {
        page.data.value = reducer(page.data.value, payload);
      }
    });
  });
  onMissedEvents(() => {
    void page.refresh();
  });

  return page;
}
