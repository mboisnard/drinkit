import { defineNuxtPlugin, useAppConfig } from '#app';
import { useToast } from '@nuxt/ui/composables/useToast';
import { errorMessageOf } from '../error-message.ts';

// The error page already shows a fatal error
export default defineNuxtPlugin((nuxtApp) => {
  const toast = useToast();
  // Any status the app adds, beside those of the starter
  const errorMessages: Record<string, string | undefined> = useAppConfig().observability.errorMessages;

  nuxtApp.hook('drinkit:error', ({ error, problem, fatal }) => {
    if (fatal) {
      return;
    }

    // A call that got no answer has no status: ofetch throws a FetchError without a response
    const key = problem.status ?? (error instanceof Error && error.name === 'FetchError' ? 'network' : 'default');
    const message = errorMessageOf(error) ?? errorMessages[key] ?? errorMessages.default;
    const reference = problem.traceId && { id: problem.traceId, description: `Reference: ${problem.traceId}` };
    toast.add({ title: message, color: 'error', ...reference });
  });
});
