/** The sign-in protocol of the backend `security-starter`, under the path of the API. */
export const AUTH_ENDPOINTS = {
  code: '/api/auth/code',
  codeSignIn: '/api/auth/code/sign-in',
  signOut: '/api/auth/logout',
} as const;
