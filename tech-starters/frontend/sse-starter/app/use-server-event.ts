import { onScopeDispose } from 'vue';
import { useNuxtApp } from '#app';

/**
 * Calls `handler` with the payload of each event of that name the backend sends, until the component unmounts. Every
 * listener of the tab shares one connection, open while one of them listens and the tab is visible.
 *
 * The handler declares the type of the payload, a JSON value the API contract does not describe and nothing checks at
 * runtime: `(chunk: AnswerChunk) => ...`.
 */
export function useServerEvent(name: string, handler: (payload: never) => void): void {
  if (import.meta.client) {
    // never as the parameter lets a handler declare any payload type, read from JSON as unknown
    onScopeDispose(useNuxtApp().$serverEvents.listen(name, handler as (payload: unknown) => void));
  }
}

/**
 * Calls `handler` each time the connection opens again while the component is mounted: the backend replays no event,
 * so a page reads its data again here to catch up with what it missed. The opening its mount starts in a visible tab
 * calls nothing, since the page has just read its data. Only `useServerEvent` opens the connection.
 */
export function onStreamOpen(handler: () => void): void {
  if (import.meta.client) {
    onScopeDispose(useNuxtApp().$serverEvents.onOpen(handler));
  }
}
