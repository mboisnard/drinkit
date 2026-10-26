import { defineConfig } from 'vitepress';
import { withMermaid } from "vitepress-plugin-mermaid";

// VitePress 1.6's own slugify, with the emoji a heading may open with left out of its anchor: links stay
// plain, and adding or removing an emoji breaks none of them
const emoji = /[\p{Extended_Pictographic}\u{FE0F}\u{20E3}]/gu;
const special = /[\s~`!@#$%^&*()\-_+=[\]{}|\\;:"'“”‘’<>,.?/]+/g;
const slugify = (heading: string) => heading
    .replace(emoji, '')
    .normalize('NFKD')
    .replace(/[\u0300-\u036F]/g, '')
    .replace(/[\u0000-\u001f]/g, '')
    .replace(special, '-')
    .replace(/-{2,}/g, '-')
    .replace(/^-+|-+$/g, '')
    .replace(/^(\d)/, '_$1')
    .toLowerCase();

// https://vitepress.dev/reference/site-config
export default withMermaid(
    defineConfig({
      srcDir: 'src',
      base: '/drinkit/',

      title: "Drink It",
      description: "Cellar management for beer, wine, spirits",

      markdown: {
        anchor: { slugify },
      },

      themeConfig: {

        search: {
          provider: 'local'
        },

        // https://vitepress.dev/reference/default-theme-config
        nav: [
          { text: 'Home', link: '/' },
          { text: 'Product', link: '/product/get-started' },
          { text: 'Engineering', link: '/engineering/' }
        ],

        sidebar: {
          '/product/': {
            base: '/product/',
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
          '/engineering/': {
            base: '/engineering/',
            items: [
              { text: 'Introduction', base: '/', link: 'engineering/' },

              {
                text: 'Architecture',
                base: '/engineering/architecture/',
                collapsed: false,
                items: [
                  {text: 'Monorepo', link: 'monorepo'},
                  {text: 'Modular Monolith', link: 'modular-monolith'},
                  {text: 'Hexagonal Architecture', link: 'hexagonal-architecture'},
                  {text: 'Tech Starters', link: 'tech-starters'},
                  {text: 'Event Sourcing', link: 'event-sourcing'},
                  {text: 'Platform Events', link: 'platform-events'},
                ],
              },

              {
                text: 'Build and Data',
                base: '/engineering/build-and-data/',
                collapsed: false,
                items: [
                  {text: 'Build Tool', link: 'build-tool'},
                  {text: 'Database', link: 'database'},
                  {text: 'Logging', link: 'logging'},
                ],
              },

              {
                text: 'Writing Code',
                base: '/engineering/writing-code/',
                collapsed: false,
                items: [
                  {
                    text: 'Best Practices',
                    collapsed: true,
                    base: '/engineering/writing-code/best-practices/',
                    items: [
                      {text: 'Software Craftsmanship Manifesto', link: 'craft-manifesto'},
                      {text: 'SOLID', link: 'solid'},
                      {text: 'Coding Rules', link: 'coding-rules'},
                      {text: 'Scout Rule', link: 'scout-rule'},
                      {text: 'Living Documentation', link: 'living-documentation'},
                      {text: 'Developer Experience', link: 'dev-exp'},
                    ],
                  },
                  {
                    text: 'Testing',
                    collapsed: true,
                    base: '/engineering/writing-code/testing/',
                    items: [
                      {text: 'Test Double', link: 'testdouble'},
                      {text: 'Test Containers', link: 'testcontainers'},
                      {text: 'Randomization', link: 'randomization'},
                      {text: 'Architecture Tests', link: 'architecture-tests'},
                    ],
                  },
                ],
              },

              {
                text: 'Delivery',
                base: '/engineering/delivery/',
                collapsed: false,
                items: [
                  { text: 'CI/CD', link: 'ci-cd' },
                  { text: 'Harness', link: 'harness' },
                ],
              },

              {
                text: 'Decision Records',
                base: '/engineering/adr/',
                collapsed: true,
                items: [
                  { text: 'About ADRs', base: '/engineering/', link: 'adr/' },
                  { text: '0001. Architecture tests with Konture', link: '0001-architecture-tests-with-konture' },
                  { text: '0002. Base, DDL and DML changelogs', link: '0002-base-ddl-and-dml-changelogs-checked-by-squawk' },
                ],
              },

              {
                text: 'Tech Radars',
                base: '/engineering/tech-radars/',
                collapsed: true,
                items: [
                  { text: 'What is it', base: '/engineering/', link: 'tech-radars/' },
                  { text: 'Frontend Tech Radar', link: 'frontend' },
                  { text: 'Backend Tech Radar', link: 'backend' },
                  { text: 'Platform Tech Radar', link: 'platform' },
                ],
              },

              {
                text: 'Security',
                base: '/engineering/security/',
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

              {
                text: 'Resources',
                base: '/engineering/resources/',
                collapsed: true,
                items: [
                  { text: 'Domains', link: 'domains/domains' },
                  { text: 'Tech Starters', link: 'tech-starters/tech-starters' },
                ],
              },

              { text: 'Ideas', link: 'ideas' },
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
          message: 'Apache License 2.0',
          copyright: 'Copyright © 2024-present | Crafted by Mathieu Boisnard with 🍺🥃🍷❤️'
        },
      }
    })
)
