import { noUnguardedBrowserGlobal } from './no-unguarded-browser-global.ts';
import { ruleTester } from './rule-tester.ts';

const filename = '/app/composables/useWidth.ts';

ruleTester.run('no-unguarded-browser-global', noUnguardedBrowserGlobal, {
  valid: [
    { code: 'function useWidth() { onMounted(() => window.innerWidth); }', filename },
    { code: 'function useWidth() { if (import.meta.client) { return window.innerWidth; } }', filename },
    { code: 'function useWidth() { return import.meta.client ? window.innerWidth : 0; }', filename },
    { code: 'function useLink() { function open() { window.open("/"); } return open; }', filename },
    { code: 'const width = typeof window === "undefined" ? 0 : 1;', filename },
    { code: 'function useWidth(window) { return window.innerWidth; }', filename },
  ],
  invalid: [
    { code: 'const width = window.innerWidth;', filename, errors: [{ messageId: 'unguarded' }] },
    {
      code: 'function useTheme() { return localStorage.getItem("theme"); }',
      filename,
      errors: [{ messageId: 'unguarded' }],
    },
    {
      code: '<script setup>\nconst title = document.title;\n</script>',
      filename: '/app/pages/index.vue',
      errors: [{ messageId: 'unguarded' }],
    },
  ],
});
