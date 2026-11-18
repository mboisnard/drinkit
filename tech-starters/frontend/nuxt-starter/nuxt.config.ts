import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  components: {
    dirs: [],
  },
  imports: {
    autoImport: false,
  },
  // Their names carry a hash of their content
  routeRules: {
    '/_nuxt/**': { headers: { 'cache-control': 'public, max-age=31536000, immutable' } },
  },
  // The browsers of .browserslistrc, which the lint checks against
  vite: {
    build: { target: 'baseline-widely-available' },
  },
  // No enum, namespace nor parameter property: TypeScript that only strips its types
  typescript: {
    tsConfig: { compilerOptions: { erasableSyntaxOnly: true } },
    // Relative to .nuxt: the lint config, which Nuxt leaves out of its tsconfigs, runs on Node
    nodeTsConfig: {
      include: ['../eslint.config.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
});
