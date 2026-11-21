import { defineNuxtPlugin } from '#app';
import { useToast } from '@nuxt/ui/composables/useToast';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import { errorMessageOf } from '../error-message.ts';

// The error page already shows a fatal error
export default defineNuxtPlugin((nuxtApp) => {
  const toast = useToast();
  const { t, exists } = useTranslate();

  nuxtApp.hook('drinkit:error', ({ error, problem, fatal }) => {
    if (fatal) {
      return;
    }

    // A call that got no answer has no status: ofetch throws a FetchError without a response. The app gives a status
    // its own message with a key of the catalog, such as observability.error.409
    const kind = problem.status ?? (error instanceof Error && error.name === 'FetchError' ? 'network' : 'default');
    const key = `observability.error.${String(kind)}`;
    const message = errorMessageOf(error) ?? (exists(key) ? t(key) : t('observability.error.default'));
    const reference = problem.traceId
      && { id: problem.traceId, description: t('observability.reference', { traceId: problem.traceId }) };
    toast.add({ title: message, color: 'error', ...reference });
  });
});
