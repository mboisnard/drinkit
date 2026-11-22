import { ref } from 'vue';
import type { Ref } from 'vue';
import { navigateTo } from '#app';
import { useSubmission } from '@drinkit/api-client-starter/use-submission';
import type { Submission } from '@drinkit/api-client-starter/use-submission';
import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
import { completeUserInformation } from '#api';

export interface RegistrationForm extends Omit<Submission, 'submit'> {
  firstname: Ref<string>;
  lastname: Ref<string>;
  birthdate: Ref<string>;
  register: () => Promise<void>;
}

/** The information a new account gives before it reaches the cellars, which the backend then makes a user's. */
export function useRegistrationForm(): RegistrationForm {
  const { refresh } = useCurrentUser();
  const { pending, problem, submit } = useSubmission();
  const firstname = ref('');
  const lastname = ref('');
  const birthdate = ref('');

  return {
    firstname,
    lastname,
    birthdate,
    pending,
    problem,
    register: () => submit(async (api) => {
      await completeUserInformation({
        ...api,
        body: { firstname: firstname.value, lastname: lastname.value, birthdate: birthdate.value },
      });
      await refresh();
      await navigateTo('/cellars', { replace: true });
    }),
  };
}
