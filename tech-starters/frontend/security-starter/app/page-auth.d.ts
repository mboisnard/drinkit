import type { PageAuth } from './types.ts';

declare module '#app' {
  interface PageMeta {
    /** Who may open the page, as a literal the build reads: a page without it fails the type check. */
    auth: PageAuth;
  }
}

export {};
