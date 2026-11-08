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
                  {text: 'Clean Architecture', link: 'clean-architecture'},
                  {text: 'Contract First', link: 'contract-first'},
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
                      {text: 'Test Doubles', link: 'testdouble'},
                      {text: 'Testcontainers', link: 'testcontainers'},
                      {text: 'Randomization', link: 'randomization'},
                      {text: 'Architecture Tests', link: 'architecture-tests'},
                    ],
                  },
                ],
              },

              {
                text: 'Coding Assistants',
                base: '/engineering/coding-assistants/',
                collapsed: false,
                items: [
                  { text: 'Harness', link: 'harness' },
                  { text: 'Hooks', link: 'hooks' },
                  { text: 'Skills', link: 'skills' },
                  { text: 'Subagents', link: 'sub-agents' },
                  { text: 'Cost optimization', link: 'cost-optimization' },
                  { text: 'Tooling', link: 'tooling' },
                ],
              },

              {
                text: 'Delivery',
                base: '/engineering/delivery/',
                collapsed: false,
                items: [
                  { text: 'CI/CD', link: 'ci-cd' },
                  { text: 'Container images', link: 'container-images' },
                ],
              },

              {
                text: 'Decision Records',
                base: '/engineering/adr/',
                collapsed: true,
                items: [
                  { text: 'About ADRs', base: '/engineering/', link: 'adr/' },
                  { text: '0000. One repository for every project', link: '0000-keep-every-project-in-one-repository' },
                  { text: '0001. Gradle and shared conventions', link: '0001-build-with-gradle-and-shared-conventions' },
                  { text: '0002. PostgreSQL, queried with jOOQ', link: '0002-store-data-in-postgresql-and-query-it-with-jooq' },
                  { text: '0003. The schema through the updater', link: '0003-apply-the-schema-through-the-updater' },
                  { text: '0004. Architecture checked by tests', link: '0004-check-the-architecture-with-tests' },
                  { text: '0005. API contract by expand and contract', link: '0005-change-the-api-contract-by-expand-and-contract' },
                  { text: '0006. Platform events behind an abstraction', link: '0006-carry-platform-events-through-an-abstraction' },
                  { text: '0007. Cache through Spring, on Caffeine', link: '0007-cache-through-spring-on-caffeine' },
                  { text: '0008. Search behind an abstraction', link: '0008-index-through-an-abstraction-search-with-meilisearch' },
                  { text: '0009. Search rebuilds in batches', link: '0009-rebuild-search-indexes-in-batches-tracked-in-postgresql' },
                  { text: '0010. Emails over SMTP', link: '0010-send-emails-over-smtp-to-a-server-chosen-per-environment' },
                  { text: '0011. Sign in without a password', link: '0011-sign-in-without-a-password' },
                  { text: '0012. Sessions in PostgreSQL', link: '0012-keep-sessions-in-postgresql' },
                  { text: '0013. Remember me by a rotating token', link: '0013-remember-me-with-a-rotating-hashed-token' },
                  { text: '0014. CSRF token in a cookie', link: '0014-protect-against-csrf-with-a-cookie-token-and-fetch-metadata' },
                  { text: '0015. Server events over SSE', link: '0015-push-server-events-over-sse-to-a-user-or-a-session' },
                  { text: '0016. Observability on Micrometer', link: '0016-observe-through-micrometer-and-export-over-opentelemetry-on-demand' },
                  { text: '0017. Feature flags through OpenFeature', link: '0017-feature-flags-through-openfeature' },
                  { text: '0018. A/B tests behind a flag, with PostHog', link: '0018-ab-tests-behind-a-flag-with-posthog' },
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
                  { text: 'Principles', link: 'principles' },
                  { text: 'Calling the API', link: 'calling-the-api' },
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
