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

/** A page for each kind of access, as an app declares them with definePageMeta. */
export function addPages(): void {
  const router = useRouter();
  for (const [path, auth] of Object.entries(PAGES)) {
    // eslint-disable-next-line @typescript-eslint/consistent-type-assertions -- an app type-checks these with its roles
    const meta = { auth } as RouteMeta;
    router.addRoute({ path, name: path, meta, component: { render: () => h('h1', path) } });
  }
}
