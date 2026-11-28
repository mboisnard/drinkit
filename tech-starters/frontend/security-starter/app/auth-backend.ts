import { useApi } from '@drinkit/api-client-starter/use-api';
import { AUTH_ENDPOINTS } from '../shared/auth-endpoints.ts';
import { useAuthConfig } from './auth-config.ts';
import type { CurrentUser } from './types.ts';

export interface EmailCode {
  email: string;
  code: string;
  /** Keeps the user signed in once the session ends, through the remember-me cookie of the backend. */
  rememberMe: boolean;
}

export interface AuthBackend {
  /** The signed-in user. A visitor gets the `ProblemError` of a 401. */
  currentUser: () => Promise<CurrentUser>;
  /** Has the backend send a code to the email: it answers the same whether an account exists or not. */
  requestCode: (email: string) => Promise<void>;
  /** Opens the session. A refused code throws the `ProblemError` of the backend. */
  signInWithCode: (emailCode: EmailCode) => Promise<void>;
  signOut: () => Promise<void>;
  /** The options of a new passkey for the signed-in user, whose challenge the session keeps. */
  passkeyCreationOptions: () => Promise<PublicKeyCredentialCreationOptionsJSON>;
  /** Saves the new passkey under its label. A refused passkey throws the `ProblemError` of a 400. */
  createPasskey: (credential: RegistrationResponseJSON, label: string) => Promise<void>;
  /** The options of a passkey sign-in, which name no passkey, so that the browser offers them all. */
  passkeySignInOptions: () => Promise<PublicKeyCredentialRequestOptionsJSON>;
  /** Opens the session. A refused passkey throws the `ProblemError` of a 401. */
  signInWithPasskey: (credential: AuthenticationResponseJSON) => Promise<void>;
}

/**
 * Every call the starter makes to the backend, outside the generated client, which belongs to the app: through
 * `useApi()`, so that each call carries the cookies of the page and the CSRF token.
 */
export function useAuthBackend(): AuthBackend {
  const { baseUrl, onRequest, onResponse } = useApi();
  const { currentUserPath } = useAuthConfig();

  function send<T = void>(method: 'GET' | 'POST', path: string, body?: object): Promise<T> {
    return $fetch<T>(path, { method, body, baseURL: baseUrl, onRequest, onResponse });
  }

  return {
    currentUser: () => send<CurrentUser>('GET', currentUserPath),
    requestCode: (email) => send('POST', AUTH_ENDPOINTS.code, { email }),
    signInWithCode: (emailCode) => send('POST', AUTH_ENDPOINTS.codeSignIn, emailCode),
    signOut: () => send('POST', AUTH_ENDPOINTS.signOut),
    passkeyCreationOptions: () => send('POST', AUTH_ENDPOINTS.passkeyCreationOptions),
    createPasskey: (credential, label) => send('POST', AUTH_ENDPOINTS.passkeyCreation, { publicKey: { credential, label } }),
    passkeySignInOptions: () => send('POST', AUTH_ENDPOINTS.passkeySignInOptions),
    signInWithPasskey: (credential) => send('POST', AUTH_ENDPOINTS.passkeySignIn, credential),
  };
}
