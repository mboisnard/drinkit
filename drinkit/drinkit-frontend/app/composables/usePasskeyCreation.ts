import { useToast } from '@nuxt/ui/composables/useToast';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import { withErrorMessage } from '@drinkit/observability-starter/error-message';
import { usePasskeys } from '@drinkit/security-starter/use-passkeys';

/** Creates a passkey of the signed-in user on this device, told in a toast, then runs `onCreated`. */
export function usePasskeyCreation(onCreated?: () => Promise<void>): () => Promise<void> {
  const { create } = usePasskeys();
  const toast = useToast();
  const { t, d } = useTranslate();

  return withErrorMessage(async () => {
    // shortcut: the date of the label is in UTC, as the app knows no time zone of the user; take theirs once the
    // account keeps one
    await create(t('passkeys.defaultLabel', { date: d(new Date(), 'date', 'UTC') }));
    toast.add({ title: t('passkeys.created'), color: 'success' });
    await onCreated?.();
  }, t('passkeys.addFailed'));
}
