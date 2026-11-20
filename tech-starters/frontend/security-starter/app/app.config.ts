import { defineAppConfig } from 'nuxt/app';

// The page of a backend that gives no answer: the app overrides it
export default defineAppConfig({
  auth: { unavailableMessage: 'The server cannot be reached. Try again in a moment.' },
});
