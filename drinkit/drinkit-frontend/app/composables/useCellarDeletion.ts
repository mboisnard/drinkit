import { useSubmission } from '@drinkit/api-client-starter/submission';
import type { Submission } from '@drinkit/api-client-starter/submission';
import { deleteCellar } from '#api';

export interface CellarDeletion extends Omit<Submission, 'submit'> {
  remove: (cellarId: string) => Promise<void>;
}

/** The deletion of a cellar, run once by the backend however often it is sent, then `onDeleted`. */
export function useCellarDeletion(onDeleted: () => Promise<void>): CellarDeletion {
  const { pending, problem, submit } = useSubmission();

  return {
    pending,
    problem,
    remove: (cellarId) => submit(async (api) => {
      await deleteCellar({ ...api, path: { cellarId } });
      await onDeleted();
    }),
  };
}
