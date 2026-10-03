# Commands of implement-issue

`<issue>` is the bare number. `gh api` fills `{owner}/{repo}` in. The board is the `projects:` entry of the forms
in `.github/ISSUE_TEMPLATE/`, read as `<owner>/<number>`.

## Mode

```
main=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
gh pr list --state open --json number,headRefName --jq '.[] | select(.headRefName | startswith("<issue>-"))'
git -C "$main" worktree list
git -C "$main" branch --list "<issue>-*"
```

A branch without a worktree gets one: `git -C "$main" fetch origin <branch>`, then
`git -C "$main" worktree add "$main/../drinkit-worktrees/<branch>" <branch>`.

## Issue

```
gh issue view <issue> --json state,projectItems --jq '[.state, .projectItems[].status.name]'
gh api 'repos/{owner}/{repo}/issues/<issue>/dependencies/blocked_by' --jq '[.[] | select(.state == "open") | .number]'
gh issue view <issue> --comments
gh api 'repos/{owner}/{repo}/issues/<issue>/parent' --jq .number
```

## Worktree and board

```
main=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
git -C "$main" fetch origin master
git -C "$main" worktree add --no-track -b <branch> "$main/../drinkit-worktrees/<branch>" origin/master
url=$(gh issue view <issue> --json url --jq .url)
gh project item-edit <number> --owner <owner> --url "$url" --field Status --value "In progress"
gh issue view <issue> --json projectItems --jq '.projectItems[].status.name'
```

## Lanes

```
git diff --name-only origin/master...HEAD
```

Match the files against `.github/workflows/config/ci-lanes.yml`.

## Judge prompt

```
Issue: <issue>
Worktree: <absolute path>
Base: <git merge-base origin/master HEAD>
Head: <git rev-parse HEAD>
Pull request draft: <absolute path of claude/pr-body.md>
Test list: <absolute path of claude/test-list.md>
Previous gaps, from the second round on:
- <gap as the judge wrote it>: <the commit and the command output that close it>
```

The `security-reviewer` prompt holds the worktree, the base and the head, nothing else.

## Pull request and CI

```
git push -u origin HEAD
gh pr create --base master --title "$(git log --reverse --format=%s origin/master..HEAD | head -n 1)" --body-file "$(git rev-parse --path-format=absolute --git-path claude/pr-body.md)"
gh project item-edit <number> --owner <owner> --url "$url" --field Status --value "In review"
gh pr checks <pr> --watch
gh pr checks <pr>
gh run view <run> --log-failed
```

Run `--watch` in the background, then read the outcome with the plain `gh pr checks`.

## Follow-up

```
gh pr checks <pr>
gh pr view <pr> --comments
gh api 'repos/{owner}/{repo}/pulls/<pr>/comments'
```
