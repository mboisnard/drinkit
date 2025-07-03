import { defineConfig } from 'vitepress'

// https://vitepress.dev/reference/site-config
export default defineConfig({
  srcDir: 'src',
  base: '/drinkit/',

  title: "Drink It",
  description: "Cellar management for beer, wine, spirits",

  themeConfig: {

    search: {
      provider: 'local'
    },

    // https://vitepress.dev/reference/default-theme-config
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/get-started' },
      { text: 'Tech', link: '/tech/' }
    ],

    sidebar: {
      '/guide/': {
        base: '/guide/',
        items: [
          {
            text: 'Introduction',
            collapsed: false,
            items: [
              {text: 'Getting Started', link: 'get-started'},
            ]
          },
        ]
      },
      '/tech/': {
        base: '/tech/',
        items: [
          {
            text: 'Guidelines',
            base: '/tech/guidelines/',
            collapsed: false,
            items: [
              {text: 'Monorepo', link: 'monorepo'},
              {text: 'Build Tool', link: 'build-tool'},
              {text: 'Modular Monolith', link: 'modular-monolith'},
              {text: 'Hexagonal Architecture', link: 'hexagonal-architecture'},
              {text: 'Tech Starters', link: 'tech-starters'},
              {text: 'Events', link: 'events'},
              {text: 'Logging', link: 'logging'},
              {text: 'Database', link: 'database'},
              {
                text: 'Best Practices',
                collapsed: false,
                base: '/tech/guidelines/best-practices/',
                items: [
                  {text: 'SOLID', link: 'solid'},
                  {text: 'Strong Type', link: 'strong-type'},
                  {text: 'Scout Rule', link: 'scout-rule'},
                  {text: 'Developer Experience', link: 'dev-exp'},
                ],
              },
              {
                text: 'Testing',
                collapsed: false,
                base: '/tech/guidelines/testing/',
                items: [
                  {text: 'Test Containers', link: 'testcontainers'},
                  {text: 'Test Double', link: 'testdouble'},
                  {text: 'Randomization', link: 'randomization'},
                ],
              },
            ],
          },

          {
            text: 'Tech Radars',
            base: '/tech/tech-radars/',
            collapsed: true,
            items: [
              { text: 'What is it', link: 'what-is-it' },
              { text: 'Frontend Tech Radar', link: 'frontend' },
              { text: 'Backend Tech Radar', link: 'backend' },
              { text: 'Platform Tech Radar', link: 'platform' },
            ],
          },

          {
            text: 'Security',
            base: '/tech/security/',
            collapsed: true,
            items: [
              { text: 'Stateful vs Stateless', link: 'stateful-stateless' },
              { text: 'CSRF', link: 'csrf' },
              { text: 'Headers', link: 'headers' },
              { text: 'CORS', link: 'cors' },
              { text: 'Login Bruteforce Protection', link: 'bruteforce-protection' },
              { text: 'Remember Me', link: 'remember-me' },
              { text: 'TOTP', link: 'totp' },
            ],
          },
        ],
      },
    },

    socialLinks: [
      { icon: 'github', link: 'https://github.com/mboisnard/drinkit' }
    ],

    editLink: {
      pattern: 'https://github.com/mboisnard/drinkit/edit/master/docs/src/:path',
      text: 'Edit this page on GitHub'
    },

    footer: {
      message: 'MIT Licensed',
      copyright: 'Copyright © 2024-present | Made by Mathieu Boisnard with 🍺🥃🍷❤️'
    },
  }
})
