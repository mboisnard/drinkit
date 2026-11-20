import { ref } from 'vue';
import type { Ref } from 'vue';
import { navigateTo } from '#app';
import { useApi } from '@drinkit/api-client-starter/use-api';
import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
import { detailOf, useSubmission } from './useSubmission';
import type { Submission } from './useSubmission';
import { completeUserInformation } from '#api';

export interface RegistrationForm extends Omit<Submission, 'submit'> {
  firstname: Ref<string>;
  lastname: Ref<string>;
  birthdate: Ref<string>;
  register: () => Promise<void>;
}

/** The information a new account gives before it reaches the cellars, which the backend then makes a user's. */
export function useRegistrationForm(): RegistrationForm {
  const api = useApi();
  const { refresh } = useCurrentUser();
  const { pending, problem, submit } = useSubmission(detailOf);
  const firstname = ref('');
  const lastname = ref('');
  const birthdate = ref('');

  return {
    firstname,
    lastname,
    birthdate,
    pending,
    problem,
    register: () => submit(async () => {
      await completeUserInformation({
        ...api,
        body: { firstname: firstname.value, lastname: lastname.value, birthdate: birthdate.value },
      });
      await refresh();
      await navigateTo('/cellars', { replace: true });
    }),
  };
}
