import type { ConfigOptions } from '@nuxt/test-utils/playwright';
import { defineConfig, devices } from '@playwright/test';
import type { PlaywrightTestConfig } from '@playwright/test';

/** The UI tests of a Nuxt app, under `test/ui`: a headless Chromium on the app built and served as in production. */
export function defineUiTestConfig(): PlaywrightTestConfig<ConfigOptions> {
  return defineConfig<ConfigOptions>({
    testDir: 'test/ui',
    forbidOnly: Boolean(process.env.CI),
    reporter: process.env.CI ? [['github'], ['html', { open: 'never' }]] : 'list',
    use: {
      // Served over HTTP even where the shell sets a certificate for Nitro, which the test runner cannot reach
      nuxt: { rootDir: process.cwd(), build: true, env: { NITRO_SSL_CERT: '', NITRO_SSL_KEY: '' } },
      trace: 'retain-on-failure',
      screenshot: 'only-on-failure',
    },
    projects: [{ name: 'chromium', use: devices['Desktop Chrome'] }],
  });
}
