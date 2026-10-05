# session-context receives Claude Code's SessionStart input and prints what Claude is told, from a sample repository.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/session-context
    repo=$work/repo
    mkdir "$work/outside"
    git init -q -b master "$repo"
    git -C "$repo" commit -q --allow-empty -m init
    git -C "$repo" update-ref refs/remotes/origin/master HEAD
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
