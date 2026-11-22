import { defineNuxtPlugin, useRuntimeConfig } from '#app';

// The path of the stream in the backend's sse-starter, under the path of its API
const STREAM_PATH = '/sse/event-stream';
// EventSource.OPEN
const OPEN = 1;

interface Listener {
  name: string;
  listener: (event: Event) => void;
}

/**
 * One connection per tab, open while a component listens and the tab is visible, as ADR 015 asks so that the session
 * of a tab nobody looks at can expire. The sign-in renews the session, so the stream opens again on it.
 */
export default defineNuxtPlugin((nuxtApp) => {
  const url = `${useRuntimeConfig().public.apiClient.path}${STREAM_PATH}`;
  const listeners = new Set<Listener>();
  const openHandlers = new Set<() => void>();
  // Kept after the browser gives up on a stream the backend refused: the tab shown again or a sign-in reopens it
  let source: EventSource | null = null;

  function open(): void {
    if (source !== null || listeners.size === 0 || document.visibilityState === 'hidden') {
      return;
    }

    const opened = new EventSource(url);
    opened.addEventListener('open', () => {
      openHandlers.forEach((handler) => {
        handler();
      });
    });
    listeners.forEach(({ name, listener }) => {
      opened.addEventListener(name, listener);
    });
    source = opened;
  }

  function close(): void {
    source?.close();
    source = null;
  }

  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'hidden') {
      close();
    } else {
      open();
    }
  });
  nuxtApp.hook('drinkit:signed-in', () => {
    close();
    open();
  });
  nuxtApp.hook('drinkit:signed-out', close);

  return {
    provide: {
      serverEvents: {
        listen(name: string, handler: (payload: unknown) => void): () => void {
          const entry: Listener = {
            name,
            listener: (event) => {
              // Thrown in a listener of EventSource, it would reach no handler of the app
              try {
                handler(JSON.parse((event as MessageEvent<string>).data));
              } catch (error) {
                nuxtApp.vueApp.config.errorHandler?.(error, null, `server event ${name}`);
              }
            },
          };
          listeners.add(entry);
          source?.addEventListener(name, entry.listener);
          open();

          return () => {
            listeners.delete(entry);
            source?.removeEventListener(name, entry.listener);

            if (listeners.size === 0) {
              close();
            }
          };
        },
        onOpen(handler: () => void): () => void {
          // The page reads its data as it mounts: the opening this mount starts in a visible tab is no reconnection
          let mountOpening = document.visibilityState === 'visible' && source?.readyState !== OPEN;
          function onOpening(): void {
            if (mountOpening) {
              mountOpening = false;
            } else {
              handler();
            }
          }
          openHandlers.add(onOpening);

          return () => {
            openHandlers.delete(onOpening);
          };
        },
      },
    },
  };
});
