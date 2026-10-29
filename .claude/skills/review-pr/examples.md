# Commands of review-pr

`<pr>` is the pull request number. `gh api` fills `{owner}/{repo}` in.

## Gather

```
gh pr view <pr> --json title,body,author,headRefName,closingIssuesReferences,files
gh pr view <pr> --json body --jq .body | grep -oiE '(closes|part of|follows up) #[0-9]+'
gh pr diff <pr>
gh pr checks <pr>
```

`closingIssuesReferences` lists only the issues a `Closes` link closes: the `grep` also finds the `Part of` and
`Follows up` links that `AGENTS.md` allows.

## Worktree

```
main=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
git -C "$main" fetch origin master +pull/<pr>/head:pr-<pr>
git -C "$main" worktree add "$main/../drinkit-worktrees/pr-<pr>" pr-<pr>
git -C "$main" merge-base origin/master pr-<pr>
git -C "$main" rev-parse pr-<pr>
```

Keep the `pr-` prefix: the project's hooks run no build in such a worktree. Remove it and its branch at the end:

```
git -C "$main" worktree remove "$main/../drinkit-worktrees/pr-<pr>"
git -C "$main" branch -D pr-<pr>
```

## Review

Write it to `$(git rev-parse --path-format=absolute --git-path claude/review.json)`, then post it:

```json
{
  "event": "COMMENT",
  "body": "2 Blocking, 1 Important, 3 Minor.",
  "comments": [
    { "path": "drinkit/drinkit-domain/src/main/kotlin/...", "line": 42, "side": "RIGHT", "body": "..." }
  ]
}
```

```
gh api 'repos/{owner}/{repo}/pulls/<pr>/reviews' --input "$(git rev-parse --path-format=absolute --git-path claude/review.json)"
```
