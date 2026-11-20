import { defineAppConfig } from 'nuxt/app';

// The toast of an unhandled error, by status, unless its caller gave one: the app overrides or adds any status
const errorMessages: Record<string, string> & { default: string } = {
  default: 'Something went wrong. Try again.',
  network: 'The server cannot be reached. Check your connection and try again.',
  401: 'Your session has ended. Sign in again.',
  403: 'You are not allowed to do this.',
  502: 'The server is unavailable. Try again in a moment.',
  503: 'The server is unavailable. Try again in a moment.',
  504: 'The server is unavailable. Try again in a moment.',
};

export default defineAppConfig({
  observability: { errorMessages },
});
