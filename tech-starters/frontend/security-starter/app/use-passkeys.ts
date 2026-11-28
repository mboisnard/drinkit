import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useAuthBackend } from './auth-backend.ts';
import { useCompleteSignIn } from './complete-sign-in.ts';

export interface Passkeys {
  /** Whether this browser creates and uses passkeys: without it, the app keeps to the code by email. */
  supported: () => boolean;
  /**
   * Opens the browser's dialog of the passkeys of the site, on a click, then signs in with the one the user picks and
   * goes to the return URL. A dialog the user closes signs nobody in. A refused passkey throws the `ProblemError` of
   * the backend.
   */
  signIn: () => Promise<void>;
  /** Creates a passkey of the signed-in user, under the label of their list. Call it on a click: Safari needs one. */
  create: (label: string) => Promise<void>;
}

/**
 * Signing in with a passkey, and creating one, through the browser's WebAuthn and the JSON forms of its options.
 *
 * W3C WebAuthn Level 3, 5.1.3 (create), 5.1.4 (get) and 5.1.10 (Signal API).
 */
export function usePasskeys(): Passkeys {
  const backend = useAuthBackend();
  const completeSignIn = useCompleteSignIn();

  return {
    supported,
    signIn: async () => {
      const options = await backend.passkeySignInOptions();
      const publicKey = PublicKeyCredential.parseRequestOptionsFromJSON(options);
      const credential = await navigator.credentials.get({ publicKey }).catch((error: unknown) => {
        if (endsWithoutPasskey(error)) {
          return null;
        }
        throw error;
      });
      if (!(credential instanceof PublicKeyCredential)) {
        return;
      }

      try {
        await backend.signInWithPasskey(credential.toJSON() as AuthenticationResponseJSON);
      } catch (error) {
        // So that the password manager hides a passkey the user removed from their account
        if (error instanceof ProblemError && error.status === 401 && 'signalUnknownCredential' in PublicKeyCredential) {
          const rpId = options.rpId ?? location.hostname;
          await PublicKeyCredential.signalUnknownCredential({ rpId, credentialId: credential.id });
        }
        throw error;
      }
      await completeSignIn();
    },
    create: async (label) => {
      const options = await backend.passkeyCreationOptions();
      const publicKey = PublicKeyCredential.parseCreationOptionsFromJSON(options);
      const credential = await navigator.credentials.create({ publicKey });
      if (credential instanceof PublicKeyCredential) {
        await backend.createPasskey(credential.toJSON() as RegistrationResponseJSON, label);
      }
    },
  };
}

// NotAllowedError: the user closed the dialog, or it timed out. SecurityError: the page is on an address a passkey
// cannot be tied to, such as an IP address
function endsWithoutPasskey(error: unknown): boolean {
  return error instanceof DOMException && (error.name === 'NotAllowedError' || error.name === 'SecurityError');
}

// The JSON forms of the options, which spare the base64url of each binary field
function supported(): boolean {
  return typeof PublicKeyCredential !== 'undefined' && 'parseRequestOptionsFromJSON' in PublicKeyCredential;
}
