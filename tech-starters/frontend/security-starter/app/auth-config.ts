import { useRuntimeConfig } from '#app';

interface AuthConfig {
  currentUserPath: string;
  signInPage: string;
  forbiddenRedirects: Readonly<Record<string, string>>;
}

/** The auth options of the app, which the module writes in the public runtime config. */
export function useAuthConfig(): AuthConfig {
  return useRuntimeConfig().public.auth;
}
