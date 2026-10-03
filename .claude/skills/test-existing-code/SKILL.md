---
name: test-existing-code
description: Adds tests to DrinkIt code that already works but has none, such as a tech starter, a port without its test contract or an adapter without an integration test. Use it when the task is to cover behavior that exists, not to add behavior, for which the test-driven-development skill applies.
---

# Test code that already exists

The tests pin down what the code does today, so that it can change safely later. They pass from the start, which
is why each one has to prove it can fail.

## 1. List what the code does

Read the code and its callers, then write the behaviors you find in
`$(git rev-parse --path-format=absolute --git-path claude/test-list.md)`, in the words of the domain: what a
caller observes, including errors and edge cases. Skip what only a framework does.

Start where a test pins the most behavior:

- a port: its test contract, run against the in-memory double and the real adapter, as
  [backend.md](../test-driven-development/backend.md) of the `test-driven-development` skill describes;
- a tech starter: through what an application that uses it sees, its beans, its properties and its effects,
  following the test layout of the `new-backend-tech-starter` skill.

## 2. One test per behavior, each one proven

The shapes of a test, of its Kotest assertions, of a port's contract and of a mutation are in
[examples.md](examples.md).

1. Write the test and run it. It passes, since the behavior exists.
2. Break the behavior on purpose: invert a condition, change a returned value. Run the test again: it must fail,
   for the reason its name says. Then revert only your mutation, not the rest of the file, and run it once more.
3. Tick the line.

A test that still passes with the code broken checks nothing: rewrite it.

## 3. When the code looks wrong

A test exposes what the code does, not what it should do. When the behavior looks like a bug, do not fix it in
the same change: pin the current behavior, name the test after it, and record the bug through the `new-issue`
skill, or ask the maintainer when it blocks the task.

Do not refactor before the tests are in place. Once they are, a refactor goes in a commit of its own.
