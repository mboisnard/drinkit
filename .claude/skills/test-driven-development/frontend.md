# Tests on the frontend

Work from what the user sees and does. Where code goes in the Nuxt app is in the `frontend-feature` skill.

1. A page or component test renders it, acts as a user would, clicks and types, and checks what appears. It finds
   elements by role, label or text, never by CSS class or component internals. An element a test cannot find that
   way is one a screen reader cannot use either: fix the markup, not the query.
2. A composable or plain logic, a formatting, a mapping of a response, the state a composable holds, is tested as a
   function: call it, act on it, assert on what it returns. Logic still in a template is moved out first.
3. The backend is reached in one of two ways, never through a module mock such as `vi.mock`:
   - a hand-written fake of the generated client, typed after it (`Pick<CellarsApi, 'findCellars'>`) and built
     from the generated types, holding its data in memory when the page writes then reads;
   - the real generated client at the HTTP boundary, its `Configuration` given a `fetchApi` that answers with
     responses shaped by the contract.

   A fake shared by several tests is written once. A page that builds its client inline, with `new CellarsApi()`
   for instance, cannot receive one: giving it its client from a place the test can replace is a reshape that
   comes first.

## When package.json has no test script

A frontend criterion is proven by check commands, run from the frontend app:
`npm run generate:client-api && npm run build`. The build does not check types, so a type error can pass it. Run
it before the change when it can fail, a build that breaks on an import not written yet for instance.

What only a person can see, a dialog that opens, a row that disappears after a delete, becomes a numbered step for
the maintainer in "Verification" of the pull request: what to start, what to do, what should appear. The test list
keeps a line for each such behavior, pointing to its step.
