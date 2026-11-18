import type { ProblemDetail } from './problem-detail';

declare module '#app' {
  interface RuntimeNuxtHooks {
    'drinkit:api:problem': (problem: ProblemDetail) => Promise<void> | void;
    'drinkit:signed-in': () => Promise<void> | void;
    'drinkit:signed-out': () => Promise<void> | void;
  }
}
