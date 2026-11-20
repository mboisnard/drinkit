import { abortNavigation, createError, defineNuxtRouteMiddleware, navigateTo, useNuxtApp } from '#app';
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

  await load();
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
