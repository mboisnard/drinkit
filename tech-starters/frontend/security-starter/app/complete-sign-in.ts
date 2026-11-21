import { navigateTo, useNuxtApp, useRoute } from '#app';
import { safeRedirect } from '../shared/safe-redirect.ts';
import { useCurrentUserState } from './current-user-state.ts';
import { useLocalizedPath } from './localized-path.ts';

/**
 * What every way of signing in ends with, once the backend opened the session: the user read, the
 * `drinkit:signed-in` hook told, and the visitor back on the page they asked for, if it is one of the app.
 */
export function useCompleteSignIn(): () => Promise<void> {
  const { refresh } = useCurrentUserState();
  const nuxtApp = useNuxtApp();
  const route = useRoute();
  const localizedPath = useLocalizedPath();

  return async () => {
    await refresh();
    await nuxtApp.callHook('drinkit:signed-in');
    await nuxtApp.runWithContext(() => navigateTo(safeRedirect(route.query.redirect) ?? localizedPath('/'), { replace: true }));
  };
}
