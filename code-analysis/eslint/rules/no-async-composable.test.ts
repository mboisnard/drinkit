import { noAsyncComposable } from './no-async-composable.ts';
import { ruleTester } from './rule-tester.ts';

const filename = '/app/composables/useCellars.ts';

ruleTester.run('no-async-composable', noAsyncComposable, {
  valid: [
    { code: 'function useCellars() { return useState("cellars"); }', filename },
    { code: 'async function loadCellars() { await fetch("/"); }', filename },
  ],
  invalid: [
    { code: 'async function useCellars() { await fetch("/"); }', filename, errors: [{ messageId: 'async' }] },
    { code: 'const useCellars = async () => fetch("/");', filename, errors: [{ messageId: 'async' }] },
  ],
});
