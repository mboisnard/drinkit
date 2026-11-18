import { composableUsesReactivity } from './composable-uses-reactivity.ts';
import { ruleTester } from './rule-tester.ts';

const filename = '/app/composables/useCellars.ts';

ruleTester.run('composable-uses-reactivity', composableUsesReactivity, {
  valid: [
    { code: 'import { ref } from "vue";\nfunction useCount() { return ref(0); }', filename },
    { code: 'import { useState } from "#imports";\nfunction useCellars() { return useState("c"); }', filename },
    { code: 'function useSortedCellars() { return useCellars(); }', filename },
    { code: 'function formatName(name) { return name.trim(); }', filename },
  ],
  invalid: [
    {
      code: 'function useFormattedName(name) { return name.trim(); }',
      filename,
      errors: [{ messageId: 'noReactivity' }],
    },
    {
      code: 'import { format } from "./format";\nfunction useLabel(cellar) { return format(cellar); }',
      filename,
      errors: [{ messageId: 'noReactivity' }],
    },
  ],
});
