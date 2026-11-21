import { HttpResponse } from 'msw';
import type { HttpResponseResolver } from 'msw';

export interface IdempotentEndpoint {
  /** The resolver to give an MSW handler, such as `http.delete(path, endpoint.resolver)`. */
  resolver: HttpResponseResolver;
  /** The `Idempotency-Key` of each request the endpoint received, in order. */
  keys: string[];
  /** How many times the endpoint ran: a request replayed does not count. */
  readonly executions: number;
  /** Runs the next request, then drops its answer as a network failure would, so that the client retries. */
  loseNextAnswer: () => void;
}

/**
 * An `@Idempotent` endpoint of the backend, kept in memory: a request whose key the endpoint already answered gets
 * that answer back without running again. A 5xx or a 429 is not kept, as `IdempotencyFilter` does.
 */
export function idempotentEndpoint(resolver: HttpResponseResolver): IdempotentEndpoint {
  const answers = new Map<string, Response>();
  const keys: string[] = [];
  let executions = 0;
  let losesNextAnswer = false;

  return {
    keys,
    get executions() {
      return executions;
    },
    loseNextAnswer: () => {
      losesNextAnswer = true;
    },
    resolver: async (info) => {
      const key = info.request.headers.get('idempotency-key');
      if (key !== null) {
        keys.push(key);
        const kept = answers.get(key);
        if (kept) {
          return kept.clone();
        }
      }

      executions++;
      const answer = await resolver(info) as Response | undefined;
      if (key !== null && answer && answer.status < 500 && answer.status !== 429) {
        answers.set(key, answer.clone());
      }
      if (losesNextAnswer) {
        losesNextAnswer = false;

        return HttpResponse.error();
      }

      return answer;
    },
  };
}
