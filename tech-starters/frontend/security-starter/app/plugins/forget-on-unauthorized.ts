import { defineNuxtPlugin, navigateTo, useRouter } from '#app';
import { useAuthConfig } from '../auth-config.ts';
import { useCurrentUserState } from '../current-user-state.ts';
import { useLocalizedPath } from '../localized-path.ts';

// A 401 to a signed-in user means the backend ended the session: an idle timeout, or a sign-out on another device
export default defineNuxtPlugin((nuxtApp) => {
  const { state, forget } = useCurrentUserState();
  const router = useRouter();
  const { signInPage } = useAuthConfig();
  const localizedPath = useLocalizedPath();

  nuxtApp.hook('drinkit:api:problem', async (problem) => {
    if (problem.status !== 401 || !state.value.user) {
      return;
    }

    await forget();
    const page = router.currentRoute.value;
    const signIn = { path: localizedPath(signInPage), query: { redirect: page.fullPath } };
    await nuxtApp.runWithContext(() => navigateTo(signIn));
  });
});
