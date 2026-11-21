import { h } from 'vue';
import { useRouter } from '#app';
import type { RouteMeta } from 'vue-router';

const PAGES = {
  '/': 'public',
  '/sign-in': 'public',
  '/private': 'signed-in',
  '/cellars': 'ROLE_USER',
  '/admin': 'ROLE_ADMIN',
  '/registration': 'ROLE_REGISTRATION_IN_PROGRESS',
};

/**
 * A page for each kind of access, as an app declares them with definePageMeta, named as @nuxtjs/i18n names the pages
 * of an app: a public page in English without a prefix and in French under /fr, any other page once, without one.
 */
export function addPages(): void {
  const router = useRouter();
  for (const [path, auth] of Object.entries(PAGES)) {
    // eslint-disable-next-line @typescript-eslint/consistent-type-assertions -- an app type-checks these with its roles
    const meta = { auth } as RouteMeta;
    const component = { render: () => h('h1', path) };
    if (auth !== 'public') {
      router.addRoute({ path, name: path, meta, component });
      continue;
    }

    router.addRoute({ path, name: `${path}___en`, meta, component });
    router.addRoute({ path: path === '/' ? '/fr' : `/fr${path}`, name: `${path}___fr`, meta, component });
  }
}
