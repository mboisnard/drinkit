import { ref } from 'vue';
import type { Ref } from 'vue';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';

export interface Submission {
  pending: Ref<boolean>;
  /** What the backend answered to the last refused submission, as the user reads it. */
  problem: Ref<string | undefined>;
  submit: (send: () => Promise<void>) => Promise<void>;
}

/** The sending of a form: pending while the backend answers, and its refusal shown rather than thrown. */
export function useSubmission(messageOf: (problem: ProblemError) => string): Submission {
  const pending = ref(false);
  const problem = ref<string>();

  return {
    pending,
    problem,
    submit: async (send) => {
      pending.value = true;
      problem.value = undefined;
      try {
        await send();
      } catch (error) {
        if (!(error instanceof ProblemError)) {
          throw error;
        }
        problem.value = messageOf(error);
      } finally {
        pending.value = false;
      }
    },
  };
}

/** The detail the backend gives a refusal, or its title. */
export function detailOf(problem: ProblemError): string {
  return problem.data.detail ?? problem.message;
}
