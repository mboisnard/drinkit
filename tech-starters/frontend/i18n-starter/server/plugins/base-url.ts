import { defineNitroPlugin, useRuntimeConfig } from '#imports';
import { baseUrlProblem } from '../../shared/base-url.ts';

// A deployment that forgot its origin would name localhost in the head of every page: it refuses to start instead
export default defineNitroPlugin(() => {
  if (import.meta.dev) {
    return;
  }

  const problem = baseUrlProblem(useRuntimeConfig().public.i18n.baseUrl);
  if (problem) {
    throw new Error(problem);
  }
});
