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
  // The fonts of the text and the titles are preloaded, or the page paints first in the fallback and then swaps
  fonts: {
    defaults: {
      subsets: ['latin'],
    },
    families: [
      { name: 'Inter', provider: 'google', weights: [400, 500, 600, 700], preload: true },
      { name: 'Bricolage Grotesque', provider: 'google', weights: [700, 800], preload: true },
      { name: 'Fraunces', provider: 'google', weights: [500], styles: ['italic'] },
      { name: 'JetBrains Mono', provider: 'google', weights: [400, 500] },
    ],
  },
  // The logo is an SVG of public/, which needs no resizing: IPX would look for it under the base path and miss it
  image: {
    provider: 'none',
  },
  // The icon names of the app config, the components and the content files are bundled: rendered from the
  // prerender's icon endpoint, they time out under its load and the page ships without them
  icon: {
    mode: 'svg',
    clientBundle: {
      scan: { globInclude: ['app/**/*.{vue,ts}', 'content/**/*.md', 'content/**/.navigation.yml'] },
    },
  },
  colorMode: {
    preference: 'light',
  },
  // The module gives both color modes one set of variables: colors with an alpha read on either background
  contentMermaid: {
    theme: { light: 'neutral', dark: 'dark' },
    loader: {
      init: {
        fontFamily: 'Inter, sans-serif',
        // @ts-expect-error The module types Mermaid's `themeVariables`, an `any`, as never, but passes it on to Mermaid
        themeVariables: {
          mainBkg: '#73737308',
          nodeBorder: '#a3a3a3',
          primaryBorderColor: '#a3a3a3',
          lineColor: '#737373',
          signalColor: '#737373',
          actorBkg: '#e0812c1f',
          actorBorder: '#e0812c',
          activationBkgColor: '#e0812c33',
          activationBorderColor: '#e0812c',
          noteBkgColor: '#eab30826',
          noteBorderColor: '#ca8a04',
          noteTextColor: '#a16207',
        },
      },
    },
  },
  agentDiscovery: {
    skills: {
      dir: '../.claude/skills',
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
