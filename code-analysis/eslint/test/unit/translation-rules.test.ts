import { createConfigForNuxt } from '@nuxt/eslint-config';
import { Linter } from 'eslint';
import { expect, test } from 'vitest';
import { translationRules } from '../../translation-rules.ts';

const RULES = ['vue/no-bare-strings-in-template', 'vue/no-restricted-syntax', '@typescript-eslint/no-restricted-imports'];

// With the Vue parser and plugin of the Nuxt config, which drinkitEslint() starts from
async function lint(source: string, path = 'app/components/CellarList.vue'): Promise<string[]> {
  const config = [...await createConfigForNuxt(), ...translationRules()];
  const messages = new Linter({ configType: 'flat' }).verify(source, config, path);

  return messages.map((message) => message.ruleId ?? message.message).filter((rule) => RULES.includes(rule));
}

test.each([
  '<template><p>No cellar yet.</p></template>',
  '<template><button aria-label="Sign out" /></template>',
  '<template><img src="/cellar.png" alt="A cellar"></template>',
])('a text written in a template is refused, since it would show in English whatever the locale: %s', async (component) => {
  // When & Then
  expect(await lint(component)).toEqual(['vue/no-bare-strings-in-template']);
});

test('a text of the catalog in a template is accepted', async () => {
  // When & Then
  expect(await lint('<template><p :title="t(\'cellars.title\')">{{ t(\'cellars.empty\') }}</p></template>')).toEqual([]);
});

test.each(['$t', '$d', '$n'])('the global %s of vue-i18n is refused in a template, since it takes any key', async (global) => {
  // When & Then
  expect(await lint(`<template><p>{{ ${global}('cellars.empty') }}</p></template>`)).toEqual(['vue/no-restricted-syntax']);
});

test.each(['vue-i18n', '#i18n', '#imports'])('useI18n is refused from %s, since its t takes any key', async (source) => {
  // When & Then
  expect(await lint(`import { useI18n } from '${source}';`, 'app/composables/useGreeting.ts'))
    .toEqual(['@typescript-eslint/no-restricted-imports']);
});

test('a page of a test, which no visitor reads, may hold text', async () => {
  // When & Then
  expect(await lint('<template><h1>Home</h1></template>', 'test/nuxt/pages/index.vue')).toEqual([]);
});
