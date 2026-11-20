import type { NuxtError } from 'nuxt/app';

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  /** The trace of the request in the backend's logs, when the backend gives it. */
  traceId?: string;
}

export function toProblemDetail(error: Partial<Pick<NuxtError, 'data' | 'status' | 'statusText'>>): ProblemDetail {
  const problem = isProblemDetail(error.data) ? error.data : { title: error.statusText };

  return { ...problem, status: error.status };
}

function isProblemDetail(data: unknown): data is ProblemDetail {
  if (typeof data !== 'object' || data === null) {
    return false;
  }

  const texts = ['title' in data ? data.title : undefined, 'detail' in data ? data.detail : undefined]
    .filter((text) => text !== undefined);

  return texts.length > 0 && texts.every((text) => typeof text === 'string');
}
