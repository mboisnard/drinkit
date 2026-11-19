import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: 'test/ui',
  forbidOnly: Boolean(process.env.CI),
});
