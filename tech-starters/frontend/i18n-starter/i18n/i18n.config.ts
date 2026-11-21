import { SOURCE_LOCALE } from '../shared/catalog.ts';
import { compileIcuMessage } from '../shared/icu-compiler.ts';
import { defineI18nConfig } from '#i18n';

export default defineI18nConfig(() => ({
  fallbackLocale: SOURCE_LOCALE,
  messageCompiler: compileIcuMessage,
}));
