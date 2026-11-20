import { computed } from 'vue';
import type { ComputedRef } from 'vue';
import { clearNuxtData, clearNuxtState, navigateTo, useNuxtApp } from '#app';
import { useAuthBackend } from './auth-backend.ts';
import { useAuthConfig } from './auth-config.ts';
import { CURRENT_USER_STATE, useCurrentUserState } from './current-user-state.ts';
import type { CurrentUser, Role } from './types.ts';

export interface CurrentUserSession {
  /** The signed-in user, `null` for a visitor. */
  user: ComputedRef<CurrentUser | null>;
  signedIn: ComputedRef<boolean>;
  hasRole: (role: Role) => boolean;
  /** Reads the user again, after the backend changed it, such as its roles once registered. */
  refresh: () => Promise<void>;
  /**
   * Ends the session on the backend, forgets every data of the user, and goes to the sign-in page.
   *
   * OWASP: ASVS 5.0 V7.4.1 and V14.3.1
   */
  signOut: () => Promise<void>;
}

/** The session of the backend as the pages see it, on the server and in the browser alike. */
export function useCurrentUser(): CurrentUserSession {
  const { state, refresh, forget } = useCurrentUserState();
  const backend = useAuthBackend();
  const nuxtApp = useNuxtApp();
  const { signInPage } = useAuthConfig();

  return {
    user: computed(() => state.value.user),
    signedIn: computed(() => state.value.user !== null),
    hasRole: (role) => state.value.user?.roles.includes(role) ?? false,
    refresh,
    signOut: async () => {
      await backend.signOut();
      await forget();

      // Away from the page first, so that it never renders again without its data
      await nuxtApp.runWithContext(() => navigateTo(signInPage));
      await nuxtApp.runWithContext(() => {
        clearNuxtData();
        // A deleted state breaks a module that still watches it, such as the Nuxt DevTools
        clearNuxtState((key) => key !== CURRENT_USER_STATE, { reset: true });
      });

      // Nuxt reads the payload the server rendered only at start, and leaves it in the page
      for (const payload of document.querySelectorAll('script[data-nuxt-data]')) payload.remove();
    },
  };
}
