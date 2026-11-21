import { compileIcuMessage } from '../shared/icu-compiler.ts';
import { defineI18nConfig } from '#i18n';

export default defineI18nConfig(() => ({
  fallbackLocale: 'en',
  messageCompiler: compileIcuMessage,
}));
