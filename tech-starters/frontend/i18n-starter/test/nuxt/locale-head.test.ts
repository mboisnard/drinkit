import { useNuxtApp, useRouter } from '#app';
import { afterEach, expect, test } from 'vitest';

afterEach(async () => {
  await useNuxtApp().$i18n.setLocale('en');
});

test('the page declares the language of its locale, for a screen reader and a search engine', async () => {
  // When
  await useRouter().push('/fr/cellar-welcome');

  // Then
  await expect.poll(() => document.documentElement.lang).toBe('fr-FR');
});

test('the page links its translation in each other locale', async () => {
  // When
  await useRouter().push('/cellar-welcome');

  // Then
  await expect
    .poll(() => [...document.querySelectorAll('link[rel="alternate"]')].map((link) => link.getAttribute('hreflang')))
    .toEqual(expect.arrayContaining(['en', 'fr', 'x-default']));
});
