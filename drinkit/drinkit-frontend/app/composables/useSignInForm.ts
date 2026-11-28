import { onMounted, ref } from 'vue';
import type { Ref } from 'vue';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useSubmission } from '@drinkit/api-client-starter/use-submission';
import type { Submission } from '@drinkit/api-client-starter/use-submission';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import type { TranslationKey } from '@drinkit/i18n-starter/translate';
import { useEmailCodeSignIn } from '@drinkit/security-starter/use-email-code-sign-in';
import { usePasskeys } from '@drinkit/security-starter/use-passkeys';
import { usePasskeyOffer } from './usePasskeyOffer';

export interface SignInForm extends Omit<Submission, 'submit'> {
  /** Whether this browser signs in with a passkey, known once the page is mounted. */
  canUsePasskey: Ref<boolean>;
  email: Ref<string>;
  code: Ref<string>;
  rememberMe: Ref<boolean>;
  codeSent: Ref<boolean>;
  sendCode: () => Promise<void>;
  signIn: () => Promise<void>;
  signInWithPasskey: () => Promise<void>;
}

/** The sign-in with a passkey, or by email code in two steps: the email, then the code the backend sent to it. */
export function useSignInForm(): SignInForm {
  const { requestCode, signIn } = useEmailCodeSignIn();
  const passkeys = usePasskeys();
  const offerPasskey = usePasskeyOffer();
  const { t } = useTranslate();
  const { pending, problem, submit } = useSubmission((refusal) => {
    const key = messageKeyOf(refusal);

    return key ? t(key) : undefined;
  });
  const email = ref('');
  const code = ref('');
  const rememberMe = ref(false);
  const codeSent = ref(false);

  const canUsePasskey = ref(false);
  onMounted(() => {
    canUsePasskey.value = passkeys.supported();
  });

  return {
    canUsePasskey,
    email,
    code,
    rememberMe,
    codeSent,
    pending,
    problem,
    sendCode: () => submit(async () => {
      await requestCode(email.value);
      codeSent.value = true;
    }),
    signIn: () => submit(async () => {
      await signIn({ email: email.value, code: code.value, rememberMe: rememberMe.value });
      offerPasskey();
    }),
    // Not through submit: a refused passkey has a message of its own, where submit reads every 401 as a wrong code
    signInWithPasskey: async () => {
      pending.value = true;
      problem.value = undefined;
      try {
        await passkeys.signIn();
      } catch (error) {
        if (!(error instanceof ProblemError)) {
          throw error;
        }
        const key = error.status === 401 ? 'signIn.passkeyRefused' : messageKeyOf(error);
        problem.value = key ? t(key) : error.data.detail ?? error.message;
      } finally {
        pending.value = false;
      }
    },
  };
}

// The backend answers the same 401 to every refused code, so that it never tells which part was wrong
function messageKeyOf(problem: ProblemError): TranslationKey | undefined {
  switch (problem.status) {
    case 401:
      return 'signIn.wrongCode';
    case 429:
      return 'signIn.tooManyAttempts';
    default:
      return undefined;
  }
}
