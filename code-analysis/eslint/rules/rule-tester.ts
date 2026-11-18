import { RuleTester } from 'eslint';
import { describe, it } from 'vitest';
import vueParser from 'vue-eslint-parser';

RuleTester.describe = describe;
RuleTester.it = it;
RuleTester.itOnly = it.only;

export const ruleTester = new RuleTester({
  languageOptions: { parser: vueParser, ecmaVersion: 'latest', sourceType: 'module' },
});
