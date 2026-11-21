import { defineNuxtPlugin, useHead } from '#app';
import { useLocaleHead } from '#i18n';

// The language of the page and the links to its translations, in every render: screen readers and search engines
// read them from the server's HTML
export default defineNuxtPlugin({
  name: 'drinkit:locale-head',
  dependsOn: ['i18n:plugin'],
  setup() {
    const head = useLocaleHead({ dir: true, lang: true, seo: true });
    useHead(() => ({ htmlAttrs: head.value.htmlAttrs, link: head.value.link, meta: head.value.meta }));
  },
});
