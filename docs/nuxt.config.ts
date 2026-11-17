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
  // The icons of .navigation.yml escape the scan of the components, and would load from the Iconify API at runtime
  icon: {
    clientBundle: {
      icons: [
        'lucide:compass', 'lucide:hexagon', 'lucide:layers', 'lucide:database', 'lucide:code', 'lucide:bot',
        'lucide:git-merge', 'lucide:book-open', 'lucide:library', 'lucide:radar', 'lucide:shield', 'lucide:cpu',
      ],
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
