import { useAsyncData } from '#app';
import { ProblemError } from '@drinkit/api-client-starter/problem-error';
import { useApi } from '@drinkit/api-client-starter/use-api';
import { getConnectedUserInfo } from '#api';
import type { ConnectedUserInformation } from '#api';

// 401 for a visitor, 404 for a user who has not activated the account yet
const NO_ONE_TO_GREET = [401, 404];

/** The greeting of the signed-in user, or none for a visitor. Any other failure is left in `error`. */
export function useGreeting() {
  const api = useApi();

  return useAsyncData('greeting', async () => {
    try {
      const { data: user } = await getConnectedUserInfo(api);

      return greetingOf(user) ?? null;
    } catch (failure) {
      if (failure instanceof ProblemError && NO_ONE_TO_GREET.includes(failure.status)) {
        return null;
      }
      throw failure;
    }
  });
}

function greetingOf(user: ConnectedUserInformation): string | undefined {
  return user.firstname ? `Welcome back, ${user.firstname}` : undefined;
}
