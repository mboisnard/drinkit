import { noModuleState } from './no-module-state.ts';
import { ruleTester } from './rule-tester.ts';

ruleTester.run('no-module-state', noModuleState, {
  valid: [
    { code: 'function useCount() { const count = ref(0); return count; }', filename: '/app/composables/useCount.ts' },
    { code: 'const MAX = 3;', filename: '/app/utils/limits.ts' },
    { code: '<script setup>\nconst count = ref(0);\n</script>', filename: '/app/components/CellarCount.vue' },
  ],
  invalid: [
    { code: 'const count = ref(0);', filename: '/app/utils/count.ts', errors: [{ messageId: 'moduleState' }] },
    {
      code: 'export const state = reactive({ open: false });',
      filename: '/app/utils/state.ts',
      errors: [{ messageId: 'moduleState' }],
    },
  ],
});
