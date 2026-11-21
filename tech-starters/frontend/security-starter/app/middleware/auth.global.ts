import { abortNavigation, createError, defineNuxtRouteMiddleware, navigateTo, useNuxtApp } from '#app';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import { accessTo, hasLanguagePrefix } from '../../shared/access.ts';
import { useAuthConfig } from '../auth-config.ts';
import { useCurrentUserState } from '../current-user-state.ts';
import { useLocalizedPath } from '../localized-path.ts';

// The pages only spare a round trip: the backend checks every call, whatever a page shows
export default defineNuxtRouteMiddleware(async (to) => {
  // The server decided for the first page: the browser takes its answer from the payload
  const nuxtApp = useNuxtApp();
  if (import.meta.client && nuxtApp.isHydrating && nuxtApp.payload.serverRendered) {
    return undefined;
  }

  // Nuxt runs the middleware again to render the error page, which needs no user: a backend that hangs would be waited
  // for twice
  if (import.meta.server && nuxtApp.ssrContext?.error) {
    return undefined;
  }

  const { state, load } = useCurrentUserState();
  const { signInPage, forbiddenRedirects } = useAuthConfig();
  const { t } = useTranslate();
  const localizedPath = useLocalizedPath();

  try {
    await load();
  } catch (error) {
    if (!noAnswer(error)) {
      throw error;
    }

    // The problem the error page shows, as a backend's 503 would carry it
    const problem = { status: 503, title: t('auth.unavailable.title'), detail: t('auth.unavailable.detail') };
    throw createError({ status: 503, statusText: problem.title, data: problem, fatal: true });
  }

  const access = accessTo(to.meta.auth, state.value.user, forbiddenRedirects);

  switch (access.kind) {
    case 'sign-in':
      return navigateTo({ path: localizedPath(signInPage), query: { redirect: to.fullPath } });
    case 'redirect':
      return navigateTo(localizedPath(access.to));
    case 'forbidden':
      return abortNavigation(createError({ status: 403, statusText: t('auth.forbidden'), fatal: true }));
    case 'open':
      return hasLanguagePrefix(to.meta.auth) ? undefined : showInLanguageOfAccount(state.value.user);
  }
});

// The page has no prefix to take a language from: that of the account wins, once known to the app
async function showInLanguageOfAccount(user: object | null): Promise<undefined> {
  const i18n = useNuxtApp().$i18n;
  const language = user && 'locale' in user ? user.locale : undefined;
  const locale = i18n.localeCodes.value.find((code) => code === language);
  if (locale) {
    await i18n.setLocale(locale);
  }

  return undefined;
}

// No answer from the backend itself, or from a gateway in front of it: unavailable, rather than broken
function noAnswer(error: unknown): boolean {
  return (error instanceof Error && error.name === 'FetchError') || (error instanceof ProblemError && [502, 504].includes(error.status));
}
