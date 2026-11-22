import { useRuntimeConfig } from '#app';

interface AuthConfig {
  currentUserPath: string;
  signInPage: string;
  forbiddenRedirects: Readonly<Record<string, string>>;
}

export function useAuthConfig(): AuthConfig {
  return useRuntimeConfig().public.auth;
}
