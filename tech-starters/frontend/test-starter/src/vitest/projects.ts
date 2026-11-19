import { fileURLToPath } from 'node:url';
import { defineVitestProject } from '@nuxt/test-utils/config';
import { defineConfig } from 'vitest/config';
import type { ViteUserConfig } from 'vitest/config';

/** The tests of a Nuxt app or starter: `test/unit` in Node, `test/nuxt` in the Nuxt environment, never mixed. */
export async function defineVitestProjects(): Promise<ViteUserConfig> {
  return defineConfig({
    test: {
      projects: [
        { test: { name: 'unit', include: ['test/unit/**/*.test.ts'], environment: 'node' } },
        await defineVitestProject({
          test: {
            name: 'nuxt',
            include: ['test/nuxt/**/*.test.ts'],
            environment: 'nuxt',
            setupFiles: [fileURLToPath(new URL('nuxt-setup.ts', import.meta.url))],
          },
        }),
      ],
    },
  });
}
