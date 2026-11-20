import { defineNuxtPlugin, isNuxtError } from '#app';
import { consola } from 'consola';
import { toProblemDetail } from '@drinkit/nuxt-starter/problem-detail';

// Every error the app leaves unhandled in the browser, logged once, then handed to the drinkit:error hook
export default defineNuxtPlugin((nuxtApp) => {
  const reported = new WeakSet<object>();

  async function report(error: unknown, fatal: boolean): Promise<void> {
    const thrown = thrownBy(error);
    if (reported.has(thrown) || isAbort(thrown)) {
      return;
    }
    reported.add(thrown);

    const problem = toProblemDetail(thrown);
    const { status, title, traceId } = problem;
    // A child of consola copies its reporters: one made here follows those the app adds
    const logger = consola.withTag('drinkit');
    logger.error('Unhandled error', { status, title, traceId }, thrown);
    // A listener that fails on every error would otherwise report its own error, again and again
    try {
      await nuxtApp.callHook('drinkit:error', { error: thrown, problem, fatal });
    } catch (listenerError) {
      logger.error('A listener of drinkit:error failed', listenerError);
    }
  }

  // Nuxt shows the error page for an error of the first render, and removes its own handler once it is done. The
  // handler of another layer, such as an alerting module, keeps receiving each error
  nuxtApp.hooks.hookOnce('app:suspense:resolve', () => {
    const { config } = nuxtApp.vueApp;
    const handlerOfAnotherLayer = config.errorHandler;
    config.errorHandler = (error, instance, info) => {
      if (!isFatal(error)) {
        void report(error, false);
      }
      handlerOfAnotherLayer?.(error, instance, info);
    };
  });
  nuxtApp.hook('app:error', (error) => report(error, true));
  window.addEventListener('unhandledrejection', (event) => {
    event.preventDefault();
    void report(event.reason, false);
  });
});

// What Nuxt shows on its error page, which also calls app:error
function isFatal(error: unknown): boolean {
  return isNuxtError(error) && (error.fatal || error.unhandled);
}

// Nuxt wraps the error it shows on its error page and keeps the thrown one as its cause. A string thrown becomes an
// Error, which every listener reads the same way
function thrownBy(error: unknown): object {
  const thrown = isNuxtError(error) && error.cause instanceof Object ? error.cause : error;

  return thrown instanceof Object ? thrown : new Error(String(thrown));
}

function isAbort(error: object): boolean {
  return 'name' in error && error.name === 'AbortError';
}
