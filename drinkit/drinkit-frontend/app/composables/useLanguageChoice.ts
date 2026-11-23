import { useNuxtApp } from '#app';
import { useSubmission } from '@drinkit/api-client-starter/use-submission';
import type { Submission } from '@drinkit/api-client-starter/use-submission';
import { useCurrentUser } from '@drinkit/security-starter/use-current-user';
import { changeLocale } from '#api';

export interface Language {
  code: string;
  /** The name of the language in itself, such as Français, which a reader of any other language recognizes. */
  name: string;
}

export interface LanguageChoice extends Omit<Submission, 'submit'> {
  languages: Language[];
  choose: (code: string) => Promise<void>;
}

/** The languages the app offers, and the choice of the account's, which every page without a prefix shows. */
export function useLanguageChoice(): LanguageChoice {
  const i18n = useNuxtApp().$i18n;
  const { refresh } = useCurrentUser();
  const { pending, problem, submit } = useSubmission();

  return {
    pending,
    problem,
    languages: i18n.localeCodes.value.map((code) => ({ code, name: nameOf(code) })),
    choose: (code) => submit(async (api) => {
      const locale = i18n.localeCodes.value.find((offered) => offered === code);
      if (!locale) {
        return;
      }

      await changeLocale({ ...api, body: { locale } });
      await refresh();
      await i18n.setLocale(locale);
    }),
  };
}

function nameOf(code: string): string {
  const name = new Intl.DisplayNames([code], { type: 'language' }).of(code) ?? code;

  return name.charAt(0).toLocaleUpperCase(code) + name.slice(1);
}
