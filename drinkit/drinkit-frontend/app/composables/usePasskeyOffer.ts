import { useToast } from '@nuxt/ui/composables/useToast';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import { usePasskeys } from '@drinkit/security-starter/use-passkeys';
import { usePasskeyCreation } from './usePasskeyCreation';

const OFFERED = 'drinkit:passkey-offered';

/** Offers once per device, after a sign-in by code, to create a passkey that signs in without a code next time. */
export function usePasskeyOffer(): () => void {
  const { supported } = usePasskeys();
  const create = usePasskeyCreation();
  const toast = useToast();
  const { t } = useTranslate();

  return () => {
    if (!supported() || !firstOfferOnThisDevice()) {
      return;
    }

    toast.add({
      title: t('passkeys.offer.title'),
      description: t('passkeys.offer.description'),
      duration: 0,
      // A failure reaches the toast of observability-starter, in the words withErrorMessage gave it
      actions: [{ label: t('passkeys.offer.create'), onClick: () => void create() }],
    });
  };
}

// A browser that keeps no storage, such as a private window that refuses it, is offered at each sign-in
function firstOfferOnThisDevice(): boolean {
  try {
    if (localStorage.getItem(OFFERED)) {
      return false;
    }
    localStorage.setItem(OFFERED, 'true');
  } catch {
    // the offer shows, unremembered
  }

  return true;
}
