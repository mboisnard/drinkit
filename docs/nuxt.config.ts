import githubAlerts from './remark/github-alerts'

export default defineNuxtConfig({
  extends: ['docus'],
  modules: ['@nuxtjs/i18n', '@barzhsieh/nuxt-content-mermaid'],
  app: {
    baseURL: '/drinkit/',
  },
  site: {
    url: 'https://mboisnard.github.io',
    name: 'Drink It',
  },
  i18n: {
    baseUrl: 'https://mboisnard.github.io',
    defaultLocale: 'en',
    locales: [{ code: 'en', name: 'English' }],
  },
  content: {
    build: {
      markdown: {
        remarkPlugins: {
          'github-alerts': { instance: githubAlerts },
        },
      },
    },
  },
  robots: {
    robotsTxt: false,
  },
  mcp: {
    enabled: false,
  },
  nitro: {
    prerender: {
      crawlLinks: true,
      failOnError: true,
    },
  },
  docus: {
    assistant: {
      enabled: false,
    },
  },
})
