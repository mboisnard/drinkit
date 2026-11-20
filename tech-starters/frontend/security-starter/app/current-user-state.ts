import type { Ref } from 'vue';
import { useNuxtApp, useState } from '#app';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useAuthBackend } from './auth-backend.ts';
import type { CurrentUser } from './types.ts';

export const CURRENT_USER_STATE = 'drinkit:current-user';

interface CurrentUserState {
  loaded: boolean;
  user: CurrentUser | null;
}

interface CurrentUserStore {
  state: Readonly<Ref<CurrentUserState>>;
  load: () => Promise<void>;
  refresh: () => Promise<void>;
  forget: () => Promise<void>;
}

/**
 * The current user, read once per app: on the server for the first page, whose payload hands it to the browser, then
 * again only when the session changes. `null` is a visitor, whom the backend answers 401. The session itself stays
 * in the backend's HttpOnly cookie, which no code of the app reads.
 *
 * OWASP: ASVS 5.0 V7.2.1 and V3.3.4, Session Management Cheat Sheet
 */
export function useCurrentUserState(): CurrentUserStore {
  const state = useState<CurrentUserState>(CURRENT_USER_STATE, () => ({ loaded: false, user: null }));
  const backend = useAuthBackend();
  const nuxtApp = useNuxtApp();

  async function refresh(): Promise<void> {
    state.value = { loaded: true, user: await backend.currentUser().catch(visitorOn401) };
  }

  return {
    state,
    refresh,
    load: async () => {
      if (!state.value.loaded) {
        await refresh();
      }
    },
    forget: async () => {
      state.value = { loaded: true, user: null };
      await nuxtApp.callHook('drinkit:signed-out');
    },
  };
}

function visitorOn401(error: unknown): null {
  if (error instanceof ProblemError && error.status === 401) {
    return null;
  }

  throw error;
}
