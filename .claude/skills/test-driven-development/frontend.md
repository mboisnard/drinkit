# Tests on the frontend

Work from what the user sees and does. Where code goes in the Nuxt app is in the `frontend-feature` skill.

1. A page or component test renders it, acts as a user would, clicks and types, and checks what appears. It finds
   elements by role, label or text, never by CSS class or component internals. An element a test cannot find that
   way is one a screen reader cannot use either: fix the markup, not the query.
2. A composable or plain logic, a formatting, a mapping of a response, the state a composable holds, is tested as a
   function: call it, act on it, assert on what it returns. Logic still in a template is moved out first.
3. A test runs the real code and replaces a boundary, never a module: no `vi.mock`, `mockNuxtImport` nor
   `mockComponent`. The composables of Nuxt run for real in `test/nuxt`, and the test sets what they read: the
   `route` option of `renderSuspended`, `document.cookie`, fake timers. The backend is reached in one of three ways:
   - `registerEndpoint` of `@nuxt/test-utils/runtime`, answering the call the page makes, in a component test;
   - the `fakeBackend` fixture of `@drinkit/test-starter/playwright`, MSW handlers on a real port that the server
     render reaches, added by `fakeBackend.use(...)` for one test, in a UI test;
   - a hand-written fake of the generated functions, typed after them (`typeof findCellars`) and holding its data
     in memory when the page writes then reads. A page that builds its client inline cannot receive one:
     giving it its client from a place the test can replace is a reshape that comes first.

   A fake shared by several tests is written once. The levels and their folders are in `frontend-tests.md`, under
   `docs/content/en/1.engineering/5.writing-code/2.testing/`.

## When package.json has no test script

A frontend criterion is proven by check commands, run from the root: `pnpm --filter <package> typecheck`, then
`pnpm --filter <package> build`. Run them before the change when they can fail, a type check that breaks on an
import not written yet for instance.

What only a person can see, a dialog that opens, a row that disappears after a delete, becomes a numbered step for
the maintainer in "Verification" of the pull request: what to start, what to do, what should appear. The test list
keeps a line for each such behavior, pointing to its step.
