import { defineNuxtConfig } from 'nuxt/config';
import { githubAlerts } from './remark/github-alerts';

// The module types Mermaid's `themeVariables`, an `any`, as never, but passes it on to Mermaid
const mermaidThemeVariables = {
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
};

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
  colorMode: {
    preference: 'light',
  },
  content: {
    build: {
      markdown: {
        // Added to the languages Docus loads into Shiki: a block in a language it does not load renders as plain text
        highlight: {
          langs: ['kotlin', 'sql', 'properties', 'markdown'],
        },
        remarkPlugins: {
          'github-alerts': { instance: githubAlerts },
        },
      },
    },
  },
  // A deployment removes the chunks of the previous build: a page still on it reloads as soon as one fails, even
  // a component chunk that loads after the navigation, which 'automatic' leaves broken
  experimental: {
    emitRouteChunkError: 'automatic-immediate',
  },
  nitro: {
    prerender: {
      crawlLinks: true,
      failOnError: true,
    },
  },
  typescript: {
    // No enum, namespace nor parameter property: TypeScript that only strips its types
    tsConfig: { compilerOptions: { erasableSyntaxOnly: true } },
    // Relative to .nuxt: the files Nuxt leaves out of its tsconfigs, which the type-aware lint rules read too
    nodeTsConfig: {
      include: ['../remark/**/*.ts', '../eslint.config.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  agentDiscovery: {
    skills: {
      dir: '../.claude/skills',
    },
  },
  // The module gives both color modes one set of variables: colors with an alpha read on either background
  contentMermaid: {
    theme: { light: 'neutral', dark: 'dark' },
    loader: {
      init: {
        fontFamily: 'Inter, sans-serif',
        themeVariables: mermaidThemeVariables as never,
      },
    },
  },
  docus: {
    assistant: {
      enabled: false,
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
  i18n: {
    baseUrl: 'https://mboisnard.github.io',
    defaultLocale: 'en',
    locales: [{ code: 'en', name: 'English' }],
  },
  // The icon names of the app config, the components and the content files are bundled: rendered from the
  // prerender's icon endpoint, they time out under its load and the page ships without them
  icon: {
    mode: 'svg',
    clientBundle: {
      scan: { globInclude: ['app/**/*.{vue,ts}', 'content/**/*.md', 'content/**/.navigation.yml'] },
    },
  },
  // The logo is an SVG of public/, which needs no resizing: IPX would look for it under the base path and miss it
  image: {
    provider: 'none',
  },
  mcp: {
    enabled: false,
  },
  robots: {
    robotsTxt: false,
  },
});
