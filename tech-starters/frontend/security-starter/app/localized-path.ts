import { useRouter } from '#app';
import { hasLanguagePrefix } from '../shared/access.ts';
import { useLocalePath } from '#i18n';

/** A path of the app in the language of the page: a public page under the prefix of its locale, any other without. */
export function useLocalizedPath(): (path: string) => string {
  const router = useRouter();
  const localePath = useLocalePath();

  return (path) => (hasLanguagePrefix(router.resolve(path).meta.auth) ? localePath(path) : path);
}
