/** The bulk evaluation of OFREP that the backend `feature-management-starter` serves under the API. */
export const OFREP_EVALUATION_PATH = '/ofrep/v1/evaluate/flags';

/** The value the backend evaluated for a key, with the reason `SPLIT` when it drew a variant of an experiment. */
export interface EvaluatedFlag {
  key: string;
  value: unknown;
  reason?: string;
  variant?: string;
}

export interface BulkEvaluation {
  flags: EvaluatedFlag[];
}

/** The keys travel in the context, comma separated, since the backend evaluates only the keys it is asked. */
export function ofrepRequestOf(flags: readonly string[], experiments: readonly string[]): object {
  return { context: { flags: flags.join(','), experiments: experiments.join(',') } };
}

export function exposurePathOf(experiment: string): string {
  return `/experiments/${encodeURIComponent(experiment)}/exposures`;
}
