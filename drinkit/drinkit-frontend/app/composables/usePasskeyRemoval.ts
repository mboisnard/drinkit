import { useSubmission } from '@drinkit/api-client-starter/use-submission';
import type { Submission } from '@drinkit/api-client-starter/use-submission';
import { removePasskey } from '#api';

export interface PasskeyRemoval extends Omit<Submission, 'submit'> {
  remove: (passkeyId: string) => Promise<void>;
}

/** The removal of a passkey, which then signs in no more, then `onRemoved`. */
export function usePasskeyRemoval(onRemoved: () => Promise<void>): PasskeyRemoval {
  const { pending, problem, submit } = useSubmission();

  return {
    pending,
    problem,
    remove: (passkeyId) => submit(async (api) => {
      await removePasskey({ ...api, path: { passkeyId } });
      await onRemoved();
    }),
  };
}
