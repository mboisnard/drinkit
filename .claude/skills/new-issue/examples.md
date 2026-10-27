# Commands of new-issue

`gh api` fills `{owner}/{repo}` in. The board is the `projects:` entry of the form, read as `<owner>/<number>`.

## Search

```
gh issue list --state all --search "<two or three key words>" --json number,title,state --limit 20
```

## Labels

```
gh label list --json name --jq '.[].name'
```

## Milestone

```
gh issue view <epic> --json milestone --jq '.milestone.title'
gh api 'repos/{owner}/{repo}/milestones?state=open' --jq '.[] | [.title, .description] | @tsv'
```

Once the maintainer agrees to a new one:

```
gh api 'repos/{owner}/{repo}/milestones' -f title="<title>" -f description="<description>" --jq .title
```

## Create and link

```
board=$(gh project view <number> --owner <owner> --format json --jq .title)
gh issue create --title "<title>" --body-file "$(git rev-parse --path-format=absolute --git-path claude/issue-draft.md)" --label "<kind label>,<area labels>,<labels of the form>" --milestone "<milestone>" --project "$board"
gh project item-edit <number> --owner <owner> --url <issue url> --field Status --value "Ready"
```

With an open blocker, the status is `--value "Backlog"`.

The links take the numeric `id` of an issue, not its number:
`gh api 'repos/{owner}/{repo}/issues/<number>' --jq .id`.

```
gh api 'repos/{owner}/{repo}/issues/<epic>/sub_issues' -F sub_issue_id=<id of the new issue>
gh api 'repos/{owner}/{repo}/issues/<new issue>/dependencies/blocked_by' -F issue_id=<id of the blocker>
```

## Read back

```
gh issue view <number> --json title,labels,milestone,projectItems
gh api 'repos/{owner}/{repo}/issues/<number>/parent' --jq .number
gh api 'repos/{owner}/{repo}/issues/<number>/dependencies/blocked_by' --jq '[.[].number]'
```
