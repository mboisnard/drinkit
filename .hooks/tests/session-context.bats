# session-context receives Claude Code's SessionStart input and prints what Claude is told, from a sample repository.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/session-context
    repo=$work/repo
    mkdir "$work/outside"
    git init -q -b master "$repo"
    mkdir -p "$repo/.hooks/git"
    stub "$repo/.hooks/git/pre-push" </dev/null
    git -C "$repo" add .hooks
    git -C "$repo" commit -q -m init
    git -C "$repo" update-ref refs/remotes/origin/master HEAD
    git -C "$repo" config core.hooksPath .hooks/git
}

# A session starts with its cwd in <folder>, the repository by default.
starts() {
    run_hook_on session-start '.cwd = $cwd' --arg cwd "${1:-$repo}"
}

# Adds a worktree on a new branch <name> with one commit of its own.
worktree_with_commit() {
    git -C "$repo" worktree add -q -b "$1" "$work/$1" master
    echo "$1" >"$work/$1/$1.txt"
    git -C "$work/$1" add "$1.txt"
    git -C "$work/$1" commit -q -m "$1"
}

# Commits lines.txt, nine numbered lines, on master.
commit_lines() {
    printf '%s\n' 1 2 3 4 5 6 7 8 9 >"$repo/lines.txt"
    git -C "$repo" add lines.txt
    git -C "$repo" commit -q -m lines
}

# Adds a worktree on a new branch <name> whose one commit writes <name> on line <line> of lines.txt.
worktree_editing_line() {
    git -C "$repo" worktree add -q -b "$1" "$work/$1" master
    sed "$2s/.*/$1/" "$work/$1/lines.txt" >"$work/$1/lines.new"
    mv "$work/$1/lines.new" "$work/$1/lines.txt"
    git -C "$work/$1" commit -q -am "$1"
}

@test "master points to the issue workflow" {
    starts
    assert_output --partial "Not on an issue branch"
}

@test "a branch level with origin/master says nothing of commits ahead" {
    starts
    refute_output --partial "ahead"
}

@test "a clean tree lists no changes" {
    starts
    refute_output --partial "Uncommitted"
}

@test "an issue branch names its issue" {
    git -C "$repo" switch -q -c 382-implement-issue
    starts
    assert_output --partial "works on issue #382: read it with gh issue view 382."
}

@test "commits not on master are counted" {
    git -C "$repo" switch -q -c 382-implement-issue
    git -C "$repo" commit -q --allow-empty -m one
    git -C "$repo" commit -q --allow-empty -m two
    starts
    assert_output --partial "2 commits ahead of origin/master."
}

@test "uncommitted work is listed" {
    echo draft >"$repo/notes.md"
    starts
    assert_output --partial "?? notes.md"
}

@test "a pull request branch names the pull request" {
    git -C "$repo" switch -q -c pr-451
    starts
    assert_output --partial "reviews pull request #451: read it with gh pr view 451."
}

@test "a pull request branch is not sent to /implement-issue" {
    git -C "$repo" switch -q -c pr-451
    starts
    refute_output --partial "/implement-issue"
}

@test "a detached HEAD is named" {
    git -C "$repo" switch -q --detach HEAD
    starts
    assert_output --partial "on a detached HEAD."
}

@test "a detached HEAD is not an issue branch" {
    git -C "$repo" switch -q --detach HEAD
    starts
    assert_output --partial "Not on an issue branch"
}

@test "without worktrees nothing waits for a cleanup" {
    starts
    refute_output --partial "to clean up"
}

@test "a worktree merged by rebase waits for a cleanup" {
    worktree_with_commit 101-merged
    git -C "$repo" cherry-pick -x 101-merged >/dev/null
    git -C "$repo" update-ref refs/remotes/origin/master master
    starts
    assert_output --partial "  $work/101-merged (101-merged)"
}

@test "a worktree merged by rebase after a neighbouring change waits for a cleanup" {
    commit_lines
    worktree_editing_line 104-first 4
    worktree_editing_line 105-second 6
    git -C "$repo" cherry-pick 104-first >/dev/null
    git -C "$repo" cherry-pick 105-second >/dev/null
    git -C "$repo" update-ref refs/remotes/origin/master master
    starts
    assert_output --partial "  $work/105-second (105-second)"
}

@test "a worktree rebased on GitHub after a neighbouring change, then merged, waits for a cleanup" {
    commit_lines
    worktree_editing_line 106-behind 3
    worktree_editing_line 107-ahead 5
    git -C "$repo" cherry-pick 107-ahead >/dev/null
    git -C "$repo" branch github-106 106-behind
    git -C "$repo" rebase -q master github-106
    git -C "$repo" switch -q master
    git -C "$repo" merge -q --ff-only github-106
    git -C "$repo" update-ref refs/remotes/origin/master master
    starts
    assert_output --partial "  $work/106-behind (106-behind)"
}

@test "the cleanup is sent to /implement-issue" {
    worktree_with_commit 101-merged
    git -C "$repo" cherry-pick -x 101-merged >/dev/null
    git -C "$repo" update-ref refs/remotes/origin/master master
    starts
    assert_output --partial "to clean up with /implement-issue <issue>"
}

@test "a worktree with a commit not on master is not listed" {
    worktree_with_commit 102-in-progress
    starts
    refute_output --partial "(102-in-progress)"
}

@test "a worktree with no commit yet is not listed" {
    git -C "$repo" worktree add -q -b 103-fresh "$work/103-fresh" master
    starts
    refute_output --partial "(103-fresh)"
}

@test "the main checkout is not listed" {
    worktree_with_commit 101-merged
    git -C "$repo" cherry-pick -x 101-merged >/dev/null
    git -C "$repo" update-ref refs/remotes/origin/master master
    starts
    refute_output --partial "(master)"
}

@test "outside a repository nothing is said" {
    starts "$work/outside"
    assert_success
    refute_output
}

@test "a clone whose git hooks run hears nothing of them" {
    starts
    refute_output --partial "git hooks"
}

@test "a clone whose core.hooksPath is not .hooks/git is told the command that turns the git hooks on" {
    git -C "$repo" config --unset core.hooksPath
    starts
    assert_output --partial "The git hooks do not run here, so pre-push cannot keep master safe: core.hooksPath is not .hooks/git. Run git -C $repo config core.hooksPath .hooks/git."
}

@test "a branch whose .hooks/git holds no pre-push is told to rebase it" {
    rm "$repo/.hooks/git/pre-push"
    starts
    assert_output --partial "The git hooks do not run here, so pre-push cannot keep master safe: .hooks/git/pre-push is missing or not executable. Rebase the branch on origin/master."
}
