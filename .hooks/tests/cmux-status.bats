# cmux-status receives Claude Code's inputs in a sample repository, and calls a fake cmux that records the tab titles
# and the sidebar pills.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/cmux-status
    repo=$work/repo
    touch "$work/calls"
    stub cmux <<STUB
case \$1 in
    rename-tab)
        [ -f "$work/rename-fails" ] && exit 1
        shift
        printf 'args %s\n' "\$*" >>"$work/calls"
        for title; do :; done
        printf 'title %s\n' "\$title" >>"$work/calls"
        ;;
    set-status) printf 'pill %s %s\n' "\$2" "\$3" >>"$work/calls" ;;
    clear-status) printf 'pill %s cleared\n' "\$2" >>"$work/calls" ;;
esac
STUB
    git init -q -b master "$repo"
    git -C "$repo" commit -q --allow-empty -m init
    git -C "$repo" switch -q -c 382-implement-issue
    tab surface-1 workspace-1
}

# The session runs in the cmux tab <surface> of <workspace>. Empty, outside cmux or without a workspace.
tab() {
    export CMUX_SURFACE_ID=$1 CMUX_WORKSPACE_ID=$2
}

# Sends fixtures/<name>.json changed by a jq filter, its cwd in the repository.
sends() {
    local name=$1 filter=${2:-.}
    run_hook_on "$name" "$filter | .cwd = \$cwd" --arg cwd "$repo" "${@:3}"
}

session_starts() {
    sends session-start
}

# Claude ran <command> in Bash, with <stdout>, as the main agent or as the subagent <agent id>.
ran() {
    sends post-tool-use-bash '.tool_input.command = $command | .tool_response.stdout = $stdout | if $agent == "" then . else .agent_id = $agent end' \
        --arg command "$1" --arg stdout "${2:-}" --arg agent "${3:-}"
}

# Claude ran <command> in Bash, which exited non-zero with <error>, as the main agent or as the subagent <agent id>.
failed() {
    sends post-tool-use-failure-bash '.tool_input.command = $command | .error = $error | if $agent == "" then . else .agent_id = $agent end' \
        --arg command "$1" --arg error "$2" --arg agent "${3:-}"
}

judge_stops_with() {
    sends subagent-stop '.last_assistant_message = $message' --arg message "$1"
}

pull_request_450_opened() {
    ran "git push -u origin HEAD && gh pr create --base master --title x --body-file b.md" \
        "remote: https://github.com/mboisnard/drinkit/pull/new/382-implement-issue
https://github.com/mboisnard/drinkit/pull/450
"
}

# The last call of <kind> cmux received: title, args or pill.
last() {
    grep "^$1 " "$work/calls" | tail -n 1 | cut -d ' ' -f 2-
}

cmux_calls() {
    wc -l <"$work/calls" | tr -d ' '
}

@test "no title on master" {
    git -C "$repo" switch -q master
    session_starts
    assert_equal "$(cmux_calls)" 0
}

@test "an issue branch names the tab after its issue" {
    session_starts
    assert_equal "$(last title)" "#382 implement"
}

@test "the tab of this session is renamed" {
    session_starts
    assert_equal "$(last args)" "--workspace workspace-1 --surface surface-1 #382 implement"
}

@test "the judge starts" {
    sends subagent-start
    assert_equal "$(last title)" "#382 judge"
}

@test "the judge pill shows while it runs" {
    sends subagent-start
    assert_equal "$(last pill)" "drinkit_judge judging"
}

@test "the judge stops" {
    sends subagent-start
    judge_stops_with "VERDICT: NEEDS_WORK 1a2b3c4
### Spec: OK"
    assert_equal "$(last title)" "#382 implement"
}

@test "a NEEDS_WORK verdict" {
    judge_stops_with "VERDICT: NEEDS_WORK 1a2b3c4
### Spec: OK"
    assert_equal "$(last pill)" "drinkit_judge judge: needs work"
}

@test "an OK verdict" {
    judge_stops_with "VERDICT: OK 5d6e7f8"
    assert_equal "$(last pill)" "drinkit_judge judge OK"
}

@test "a judge without a verdict clears the pill" {
    judge_stops_with "I could not finish the review."
    assert_equal "$(last pill)" "drinkit_judge cleared"
}

@test "a subagent tool call is ignored" {
    session_starts
    calls=$(cmux_calls)
    ran "gh pr create --title x" "https://github.com/mboisnard/drinkit/pull/451" a1
    assert_equal "$(cmux_calls)" "$calls"
}

@test "a failed subagent tool call is ignored" {
    session_starts
    calls=$(cmux_calls)
    failed "gh pr checks 451" "Exit code 1
CI gate	fail	3s	https://y" a1
    assert_equal "$(cmux_calls)" "$calls"
}

@test "the pull request is opened, its number past a pull/new link" {
    pull_request_450_opened
    assert_equal "$(last title)" "#382 PR #450"
}

@test "CI starts with the pull request" {
    pull_request_450_opened
    assert_equal "$(last pill)" "drinkit_ci CI running"
}

@test "a passing CI gate" {
    ran "gh pr checks 450" "Backend	skipping	0	https://x
CI gate	pass	3s	https://y"
    assert_equal "$(last pill)" "drinkit_ci CI green"
}

@test "a pending CI gate, which gh reports with exit code 8" {
    failed "gh pr checks 450" "Exit code 8
Backend	pending	0	https://x
CI gate	pending	0	https://y"
    assert_equal "$(last pill)" "drinkit_ci CI running"
}

@test "a failing CI gate, which gh reports with exit code 1" {
    failed "gh pr checks 450" "Exit code 1
Backend	fail	2m	https://x
CI gate	fail	3s	https://y"
    assert_equal "$(last pill)" "drinkit_ci CI red"
}

@test "gh pr checks --watch keeps the last table" {
    ran "gh pr checks 450 --watch" "Refreshing checks status every 10 seconds. Press Ctrl+C to quit.

Backend	pending	0	https://x
CI gate	pending	0	https://y

Backend	pass	4m	https://x
CI gate	pass	3s	https://y"
    assert_equal "$(last pill)" "drinkit_ci CI green"
}

@test "an unchanged title calls cmux once" {
    session_starts
    calls=$(cmux_calls)
    ran "gh pr view 450"
    assert_equal "$(cmux_calls)" "$calls"
}

@test "a new tab on the same worktree is renamed" {
    pull_request_450_opened
    tab surface-2 workspace-1
    session_starts
    assert_equal "$(last args)" "--workspace workspace-1 --surface surface-2 #382 PR #450"
}

@test "without CMUX_WORKSPACE_ID only the surface is named" {
    tab surface-1 ''
    session_starts
    assert_equal "$(last args)" "--surface surface-1 #382 implement"
}

@test "a rename that failed is tried again" {
    touch "$work/rename-fails"
    session_starts
    rm "$work/rename-fails"
    session_starts
    assert_equal "$(last args)" "--workspace workspace-1 --surface surface-1 #382 implement"
}

@test "outside cmux nothing happens" {
    tab '' workspace-1
    session_starts
    assert_equal "$(cmux_calls)" 0
}

@test "without cmux installed the hook exits 0" {
    only_tools "$work/tools" cat cut git grep head jq mkdir tail
    hook_path=$work/tools
    session_starts
    assert_success
}

@test "back on master after an issue branch, the tab is left alone" {
    session_starts
    calls=$(cmux_calls)
    git -C "$repo" switch -q master
    session_starts
    assert_equal "$(cmux_calls)" "$calls"
}

@test "a detached HEAD leaves the tab alone" {
    git -C "$repo" switch -q --detach HEAD
    session_starts
    assert_equal "$(cmux_calls)" 0
}

@test "another issue does not inherit the pull request of the first" {
    pull_request_450_opened
    git -C "$repo" switch -q -c 401-bottle-label
    session_starts
    assert_equal "$(last title)" "#401 implement"
}

@test "nothing is ever printed to stdout" {
    session_starts
    refute_output
    sends subagent-start
    refute_output
    judge_stops_with "VERDICT: OK 5d6e7f8"
    refute_output
    pull_request_450_opened
    refute_output
    failed "gh pr checks 450" "Exit code 1
CI gate	fail	3s	https://y"
    refute_output
}
