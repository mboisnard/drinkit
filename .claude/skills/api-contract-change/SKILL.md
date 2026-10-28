---
name: api-contract-change
description: Changes DrinkIt's OpenAPI contract first, then the backend controllers and the frontend client generated from it, and adapts both sides in the same pull request. Use it before adding or changing an operation, a parameter, a schema or a response of the API contract, or the generator configuration of either side.
paths:
  - "drinkit/*-api-contract/**"
  - "drinkit/*-frontend/openapi-config.yml"
---

# Changing the API contract

The contract is the single description of the HTTP API, and both sides generate their code from it. A change
to it is a structuring choice: ask first, as `AGENTS.md` says.

## Where things are

- `api-definition.yaml`, in the `contract/` folder of `drinkit-api-contract`, holds every path and operation. The
  generators read this file only, and reach the others through its `$ref`s.
- The fragments next to it hold the components, one area each, and `common.yaml` what several areas share. A
  fragment is a whole OpenAPI document with `paths: {}` and `x-fragment: true` under `info`.
- The backend code is generated under the `build` folder of `drinkit-backend`, not committed, on every
  compilation.

## Steps

1. **The operation**, in `api-definition.yaml`, with an `operationId` and one tag. The tag names the backend
   `<Tag>Api` interface, which its `<Tag>Controller` implements, and the frontend `<Tag>Api` class: reuse an
   existing tag when the operation belongs to its area.
2. **The schemas**, in the fragment of the area, with `required` and the constraints the API enforces. The
   backend turns them into Bean Validation, checked before the controller runs: a request that breaks them gets a
   400. Business rules stay in the domain.
3. **A domain type for a string**: a `format` on the schema, then its two mappings in the build file of
   `drinkit-backend`, like the identifiers already mapped there. The frontend keeps a `string`.
4. **The responses**: every status the controller can answer, each with a `description`, an error body only
   the way the existing error responses describe one.
5. **Both sides**: the controller and its security rule through the `hexagonal-backend` skill, then the frontend
   client regenerated and the pages that use what changed adapted, through the `frontend-feature` skill.

## Backward compatibility

The frontend and the backend deploy separately: a browser tab opened before a deployment keeps the old client.
A change that breaks the contract never lands in one pull request, even when it adapts both sides. It goes
through expand and contract, in two pull requests:

1. Add the new element next to the old one, mark the old one `deprecated: true`, and move the frontend to the new
   one. A new required request field starts optional.
2. Once the first pull request is deployed, remove the old element, or make the new field required.

The frontend moves in the first pull request only when the backend deploys before it. Otherwise it moves in a
pull request of its own, once the first one is deployed.

The table of breaking changes, and the reasons, are on the
[contract-first page](../../../docs/src/engineering/architecture/contract-first.md). Renaming an `operationId` or
a tag changes no HTTP exchange: it renames the generated methods and classes on both sides, in one pull request.

## Check it

```
./gradlew :drinkit-backend:compileKotlin
npm run generate:client-api && npm run build    # from the frontend app
```

## Traps

- An operation that its controller does not override fails the compilation: the generated interface has no
  default body. A new tag has no controller yet, so nothing fails: its operations are not served until a
  `@RestController` implements its `<Tag>Api`.
- The two sides use separate generator versions, the backend one from the Gradle catalogs and the frontend one
  from the frontend's `openapitools.json`. A construct can generate on one side and fail on the other: build
  both.
- A schema that no `$ref` reaches from `api-definition.yaml` is not generated.

## Before you finish

- [ ] Each new operation has a tag and every status its controller can answer.
- [ ] A new `format` has both its mappings in the build file of `drinkit-backend`.
- [ ] Each operation is implemented by its controller, and the other side is adapted in the same pull request.
- [ ] Both check commands pass.
