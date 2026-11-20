import type { ProblemDetail } from '@drinkit/nuxt-starter/problem-detail';

/** A call the backend refused, with the problem it sent: Nuxt's error page reads its status and `data`. */
export class ProblemError extends Error {
  override readonly name = 'ProblemError';
  readonly status: number;
  readonly statusText: string;
  readonly data: ProblemDetail;

  constructor(response: { status: number; statusText: string }, problem: ProblemDetail) {
    super(problem.title ?? response.statusText);
    this.status = response.status;
    this.statusText = response.statusText;
    this.data = problem;
  }
}
