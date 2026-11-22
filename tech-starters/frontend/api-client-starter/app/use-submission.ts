import { ref } from 'vue';
import type { Ref } from 'vue';
import type { FetchContext } from 'ofetch';
import { retry } from '@drinkit/utils-starter/retry';
import { ProblemError } from './problem-error.ts';
import { isWrite, useApi } from './use-api.ts';
import type { ApiOptions } from './use-api.ts';

const IN_PROGRESS_RETRIES = 3;

export interface Submission {
  pending: Ref<boolean>;
  /** What the backend answered to the last refused submission, as the user reads it. */
  problem: Ref<string | undefined>;
  /**
   * Runs `send` with the options of `useApi()`, which give each of its unsafe calls an `Idempotency-Key`: the same
   * while the outcome of that call is unknown, a new one after a known answer or for another method, URL or body.
   */
  submit: (send: (api: ApiOptions) => Promise<unknown>) => Promise<void>;
}

/**
 * The sending of a form: once while the backend answers, and its refusal shown rather than thrown, in the words
 * `messageOf` gives, or else in the detail the backend gives.
 */
export function useSubmission(messageOf?: (problem: ProblemError) => string | undefined): Submission {
  const api = useApi();
  const pending = ref(false);
  const problem = ref<string>();
  const keys = new Map<string, string>();
  let retryAfterMs = 1000;

  const keyed: ApiOptions = {
    ...api,
    onRequest: async (context) => {
      await api.onRequest(context);

      const method = context.options.method?.toUpperCase() ?? 'GET';
      if (isWrite(method)) {
        const request = `${method} ${urlOf(context)}\n${bodyOf(context)}`;
        const key = keys.get(request) ?? crypto.randomUUID();
        keys.set(request, key);
        context.options.headers.set('idempotency-key', key);
      }
    },
    onResponse: async (context) => {
      retryAfterMs = Number(context.response?.headers.get('retry-after') ?? 1) * 1000;
      await api.onResponse(context);
    },
  };

  return {
    pending,
    problem,
    submit: async (send) => {
      if (pending.value) {
        return;
      }

      pending.value = true;
      problem.value = undefined;
      try {
        await retry(() => send(keyed), {
          retries: IN_PROGRESS_RETRIES,
          delayMs: () => retryAfterMs,
          retryIf: (error) => error instanceof ProblemError && error.status === 409,
        });
        keys.clear();
      } catch (error) {
        if (!(error instanceof ProblemError)) {
          throw error;
        }
        if (!isOutcomeUnknown(error.status)) {
          keys.clear();
        }
        problem.value = messageOf?.(error) ?? error.data.detail ?? error.message;
      } finally {
        pending.value = false;
      }
    },
  };
}

// The backend kept nothing for these, so the same key runs the request again
function isOutcomeUnknown(status: number): boolean {
  return status === 409 || status === 429 || status >= 500;
}

function urlOf({ request }: FetchContext): string {
  return typeof request === 'string' ? request : request.url;
}

// shortcut: a FormData or Blob body reads as `{}`, so two uploads to one URL share a key; give them a fingerprint
// of their own when an upload needs one
function bodyOf({ options }: FetchContext): string {
  return typeof options.body === 'string' ? options.body : JSON.stringify(options.body);
}
