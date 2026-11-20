import { navigateTo, useNuxtApp, useRoute } from '#app';
import { safeRedirect } from '../shared/safe-redirect.ts';
import { useCurrentUserState } from './current-user-state.ts';

/**
 * What every way of signing in ends with, once the backend opened the session: the user read, the
 * `drinkit:signed-in` hook told, and the visitor back on the page they asked for, if it is one of the app.
 */
export function useCompleteSignIn(): () => Promise<void> {
  const { refresh } = useCurrentUserState();
  const nuxtApp = useNuxtApp();
  const route = useRoute();

  return async () => {
    await refresh();
    await nuxtApp.callHook('drinkit:signed-in');
    await nuxtApp.runWithContext(() => navigateTo(safeRedirect(route.query.redirect) ?? '/', { replace: true }));
  };
}
