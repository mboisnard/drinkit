import { http, HttpResponse } from 'msw';
import type { RequestHandler } from 'msw';
import { OFREP_EVALUATION_PATH } from '../shared/ofrep.ts';
import type { EvaluatedFlag } from '../shared/ofrep.ts';

export interface OfrepBackendDoubleOptions {
  /** The path of the API, `apiClient.path` of the app, such as `/drinkit`. */
  apiPath: string;
  /** The evaluations the backend answers a request, from its session for instance. None by default. */
  evaluate?: (request: Request) => EvaluatedFlag[];
}

export interface OfrepBackendDouble {
  /** The handlers to give the fake backend of `@drinkit/test-starter`. */
  handlers: RequestHandler[];
  /** The exposures the browser reported, in their order. */
  exposures: Array<{ experiment: string; variant: string }>;
}

/** The OFREP bulk evaluation and the exposures of the backend `feature-management-starter`, kept in memory. */
export function ofrepBackendDouble({ apiPath, evaluate = () => [] }: OfrepBackendDoubleOptions): OfrepBackendDouble {
  const exposures: OfrepBackendDouble['exposures'] = [];

  return {
    exposures,
    handlers: [
      http.post(`${apiPath}${OFREP_EVALUATION_PATH}`, ({ request }) => HttpResponse.json({ flags: evaluate(request) })),
      http.post(`${apiPath}/experiments/:experiment/exposures`, async ({ request, params }) => {
        const { variant } = await request.json() as { variant: string };
        exposures.push({ experiment: String(params.experiment), variant });

        return new HttpResponse(null, { status: 204 });
      }),
    ],
  };
}
