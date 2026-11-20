import { abortNavigation, createError, defineNuxtRouteMiddleware, navigateTo, useAppConfig, useNuxtApp } from '#app';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { accessTo } from '../../shared/access.ts';
import { useAuthConfig } from '../auth-config.ts';
import { useCurrentUserState } from '../current-user-state.ts';

// The pages only spare a round trip: the backend checks every call, whatever a page shows
export default defineNuxtRouteMiddleware(async (to) => {
  // The server decided for the first page: the browser takes its answer from the payload
  const nuxtApp = useNuxtApp();
  if (import.meta.client && nuxtApp.isHydrating && nuxtApp.payload.serverRendered) {
    return undefined;
  }

  const { state, load } = useCurrentUserState();
  const { signInPage, forbiddenRedirects } = useAuthConfig();

  try {
    await load();
  } catch (error) {
    if (!noAnswer(error)) {
      throw error;
    }

    const { unavailableMessage } = useAppConfig().auth;
    throw createError({ status: 503, statusText: 'Service Unavailable', message: unavailableMessage, fatal: true });
  }

  const access = accessTo(to.meta.auth, state.value.user, forbiddenRedirects);

  switch (access.kind) {
    case 'sign-in':
      return navigateTo({ path: signInPage, query: { redirect: to.fullPath } });
    case 'redirect':
      return navigateTo(access.to);
    case 'forbidden':
      return abortNavigation(createError({ status: 403, statusText: 'Forbidden', fatal: true }));
    case 'open':
      return undefined;
  }
});

// No answer from the backend itself, or from a gateway in front of it: unavailable, rather than broken
function noAnswer(error: unknown): boolean {
  return (error instanceof Error && error.name === 'FetchError') || (error instanceof ProblemError && [502, 504].includes(error.status));
}
