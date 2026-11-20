import { useAuthBackend } from './auth-backend.ts';
import type { EmailCode } from './auth-backend.ts';
import { useCompleteSignIn } from './complete-sign-in.ts';

export type { EmailCode } from './auth-backend.ts';

export interface EmailCodeSignIn {
  /** Has the backend send a code to the email: it answers the same whether an account exists or not. */
  requestCode: (email: string) => Promise<void>;
  /** Signs in with the code, then goes to the return URL. A refused code throws the `ProblemError` of the backend. */
  signIn: (emailCode: EmailCode) => Promise<void>;
}

/** Signing in with a code sent by email, which creates the account of an unknown email. */
export function useEmailCodeSignIn(): EmailCodeSignIn {
  const backend = useAuthBackend();
  const completeSignIn = useCompleteSignIn();

  return {
    requestCode: backend.requestCode,
    signIn: async (emailCode) => {
      await backend.signInWithCode(emailCode);
      await completeSignIn();
    },
  };
}
