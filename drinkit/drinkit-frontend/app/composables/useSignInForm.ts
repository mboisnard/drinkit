import { ref } from 'vue';
import type { Ref } from 'vue';
import type { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useSubmission } from '@drinkit/api-client-starter/submission';
import type { Submission } from '@drinkit/api-client-starter/submission';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import type { TranslationKey } from '@drinkit/i18n-starter/translate';
import { useEmailCodeSignIn } from '@drinkit/security-starter/use-email-code-sign-in';

export interface SignInForm extends Omit<Submission, 'submit'> {
  email: Ref<string>;
  code: Ref<string>;
  rememberMe: Ref<boolean>;
  codeSent: Ref<boolean>;
  sendCode: () => Promise<void>;
  signIn: () => Promise<void>;
}

/** The two steps of the sign-in by email code: the email, then the code the backend sent to it. */
export function useSignInForm(): SignInForm {
  const { requestCode, signIn } = useEmailCodeSignIn();
  const { t } = useTranslate();
  const { pending, problem, submit } = useSubmission((refusal) => {
    const key = messageKeyOf(refusal);

    return key ? t(key) : undefined;
  });
  const email = ref('');
  const code = ref('');
  const rememberMe = ref(false);
  const codeSent = ref(false);

  return {
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
    signIn: () => submit(() => signIn({ email: email.value, code: code.value, rememberMe: rememberMe.value })),
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
