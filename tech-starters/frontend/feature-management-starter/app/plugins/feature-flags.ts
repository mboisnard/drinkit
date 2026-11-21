import { defineNuxtPlugin } from '#app';
import { useFeatureFlags, useFeatureFlagsLoader } from '../feature-flags.ts';

// The server render reads the flags, the browser reuses its payload, or reads them when there is none, as an SPA.
// Another subject has other flags: the sign-out hook runs once the states are back to their start
export default defineNuxtPlugin(async (nuxtApp) => {
  const flags = useFeatureFlags();
  const load = useFeatureFlagsLoader();

  if (flags.value === null) {
    await load();
  }

  if (import.meta.client) {
    nuxtApp.hook('drinkit:signed-in', load);
    nuxtApp.hook('drinkit:signed-out', load);
  }
});
