/** The sign-in protocol of the backend `security-starter`, under the path of the API: passkeys at Spring's URLs. */
export const AUTH_ENDPOINTS = {
  code: '/api/auth/code',
  codeSignIn: '/api/auth/code/sign-in',
  signOut: '/api/auth/logout',
  passkeyCreationOptions: '/webauthn/register/options',
  passkeyCreation: '/webauthn/register',
  passkeySignInOptions: '/webauthn/authenticate/options',
  passkeySignIn: '/login/webauthn',
} as const;
