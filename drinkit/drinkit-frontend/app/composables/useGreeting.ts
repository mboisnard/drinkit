import { computed } from 'vue';
import type { ComputedRef } from 'vue';
import { useTranslate } from '@drinkit/i18n-starter/translate';
import { useCurrentUser } from '@drinkit/security-starter/use-current-user';

/** The greeting of the signed-in user, or none for a visitor or a user who has not given a first name yet. */
export function useGreeting(): ComputedRef<string | undefined> {
  const { user } = useCurrentUser();
  const { t } = useTranslate();

  return computed(() => (user.value?.firstname ? t('home.greeting', { firstname: user.value.firstname }) : undefined));
}
