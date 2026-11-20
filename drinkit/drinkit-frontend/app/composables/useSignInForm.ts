import { ref } from 'vue';
import type { Ref } from 'vue';
import type { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useEmailCodeSignIn } from '@drinkit/security-starter/use-email-code-sign-in';
import { detailOf, useSubmission } from './useSubmission';
import type { Submission } from './useSubmission';

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
  const { pending, problem, submit } = useSubmission(messageOf);
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
function messageOf(problem: ProblemError): string {
  switch (problem.status) {
    case 401:
      return 'This code is wrong or has expired. Check it, or ask for a new one.';
    case 429:
      return 'Too many attempts. Wait a little before trying again.';
    default:
      return detailOf(problem);
  }
}
