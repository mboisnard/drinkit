---
name: threat-model
description: Builds a short STRIDE threat model of one DrinkIt feature or flow, such as login, registration or a cellar operation. Maps the data flows and trust boundaries from the code and the API contract, rates the threats, lists the mitigations that exist with their path:line and the gaps, then writes the result where the maintainer chooses. Started by the maintainer as /threat-model.
disable-model-invocation: true
argument-hint: <feature or flow>
---

# Threat model of $ARGUMENTS

Which threats a flow faces, which ones the code already stops, and which ones it does not. It reads the code and
the contract and runs nothing: runtime evidence is the `pentest` skill's job. One page at most, no theory.

## 1. Scope the flow

Say in one sentence what the flow does, then name its entry points: the operations of the application's API
contract, in its `-api-contract` module, by `operationId`, and anything else that starts it, a scheduled task or an
event listener. The roles of its actors are in the roles type of its `-domain` module, `Roles` in DrinkIt. More
than six operations is more than one flow: ask the maintainer which part to take.

## 2. Map the data flows

Follow one request through the code, reading each step rather than guessing it, from the contract operation to
the store or the service it reaches. Mark the trust boundaries it crosses: browser to backend, backend to
PostgreSQL, backend to a third-party service, backend to the logs. Where each control lives is in the map of the
`security-reviewer` agent.

Draw it as a Mermaid `flowchart`, one `subgraph` per trust zone, each edge labelled with the data that crosses it.

## 3. STRIDE per element

Take each process, data store and flow that crosses a boundary, and keep only the threats that apply; a letter
with none gets one line saying why. Where this project differs from the textbook:

- Repudiation: the author a command carries, `Author` in DrinkIt, and the events of an event-sourced aggregate,
  such as DrinkIt's `User`.
- Information disclosure: also the status codes that tell an existing account apart.
- Elevation of privilege: whether method security is on, which decides if a `@PreAuthorize` protects anything.

## 4. Rate, then find the mitigations

Likelihood from who can attempt it, impact from what it reaches, combined into High, Medium or Low with one line
of reason. No scoring framework.

For each threat, the mitigation that exists, with the `path:line` you read at `HEAD` and the test that proves it
when there is one. A mitigation you cannot point to does not exist. A threat with no mitigation is a gap: say what
would close it, where, and how to prove it, a test or a `pentest` check. When a guideline page and the code
disagree, the code is the truth: say so in the result.

## 5. Write it down

Ask the maintainer where the result goes: a section in the page of `docs/src/engineering/security/` whose topic
matches, or a new page in that folder with its entry in the Security block of `docs/.vitepress/config.ts`. A
mitigation the maintainer chooses among options gets its ADR through the `new-adr` skill.

The docs site is public. A gap that can be exploited today is described by the control that is missing, never as
steps to exploit it, and `SECURITY.md` applies: ask the maintainer before writing such a gap down at all.

````
## Threat model: <flow>

At <short sha>, <date>. <one sentence on the flow>

```mermaid
<the data flow diagram>
```

| Threat | STRIDE | Rating | Mitigation | Gap |
|---|---|---|---|---|
````

Write it as the `documentation-change` skill says, run its tics command on the page, then build the site and fix
any dead link. With the maintainer's OK, each gap to fix becomes an issue through the `new-issue` skill.

## Before you finish

- Every mitigation has a `path:line` you read, every rating a reason.
- Every gap has an outcome the maintainer chose: an issue, an accepted risk, or nothing written yet.
- The tics command prints nothing for the page, and the site builds.
