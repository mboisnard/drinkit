import { registerEndpoint } from '@nuxt/test-utils/runtime';
import { readBody, setResponseStatus } from 'h3';

export interface EvaluatedFlag {
  key: string;
  value: unknown;
  reason?: string;
  variant?: string;
}

/** The OFREP and exposure endpoints of the backend in the Nuxt test environment, with what they received. */
export interface OfrepBackend {
  /** The evaluations the next bulk request gets, or `'down'` for a backend that fails. */
  flags: EvaluatedFlag[] | 'down';
  /** The answer of the next bulk request, held until `until` settles. */
  slowNextAnswer?: { flags: EvaluatedFlag[]; until: Promise<unknown> };
  evaluations: unknown[];
  exposures: Array<{ experiment: string; variant: string }>;
}

export function ofrepBackend(): OfrepBackend {
  const backend: OfrepBackend = { flags: [], evaluations: [], exposures: [] };

  registerEndpoint('/drinkit/ofrep/v1/evaluate/flags', {
    method: 'POST',
    handler: async (event) => {
      backend.evaluations.push(await readBody(event));
      const slow = backend.slowNextAnswer;
      backend.slowNextAnswer = undefined;
      if (slow) {
        await slow.until;

        return { flags: slow.flags };
      }
      if (backend.flags === 'down') {
        setResponseStatus(event, 500);

        return { title: 'Internal Server Error', status: 500 };
      }

      return { flags: backend.flags };
    },
  });
  registerEndpoint('/drinkit/experiments/checkout-test/exposures', {
    method: 'POST',
    handler: async (event) => {
      const { variant } = await readBody<{ variant: string }>(event);
      backend.exposures.push({ experiment: 'checkout-test', variant });
      setResponseStatus(event, 204);

      return null;
    },
  });

  return backend;
}
