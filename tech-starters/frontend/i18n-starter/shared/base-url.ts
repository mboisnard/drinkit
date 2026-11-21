const LOCAL_HOSTS = ['localhost', '127.0.0.1', '[::1]'];

/**
 * Why a base URL cannot be the origin the links to the other languages name, or nothing: a search engine follows
 * them, so they need the public origin of the deployment, not that of a developer's machine.
 */
export function baseUrlProblem(baseUrl: string): string | undefined {
  if (!URL.canParse(baseUrl)) {
    return `i18n.baseUrl is not an absolute URL: set NUXT_PUBLIC_I18N_BASE_URL to the public origin of the app`;
  }

  const { hostname } = new URL(baseUrl);
  if (LOCAL_HOSTS.includes(hostname) || hostname.endsWith('.localhost')) {
    return `i18n.baseUrl names ${hostname}: set NUXT_PUBLIC_I18N_BASE_URL to the public origin of the app`;
  }

  return undefined;
}
