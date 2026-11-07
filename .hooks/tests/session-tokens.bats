# session-tokens reads a sample session transcript and the transcripts of its subagents, and prints their tokens.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/tools/session-tokens
    session=$work/session.jsonl
    mkdir -p "$work/session/subagents"
}

# Appends to <file> an assistant message <id> of <model> with its input, cache write, cache read and output tokens.
message() {
    jq -nc --arg id "$2" --arg model "$3" --argjson usage "[$4, $5, $6, $7]" '{type: "assistant", message: {
        id: $id, model: $model, usage: {input_tokens: $usage[0], cache_creation_input_tokens: $usage[1],
        cache_read_input_tokens: $usage[2], output_tokens: $usage[3]}}}' >>"$1"
}

# Writes the meta file of subagent <name> with its <type>.
subagent() {
    jq -nc --arg type "$2" '{agentType: $type}' >"$work/session/subagents/$1.meta.json"
}

@test "the main thread and each agent type get a row per model, with their largest context and weighted tokens" {
    message "$session" m1 claude-opus 10 100 1000 20
    subagent agent-a1 issue-judge
    message "$work/session/subagents/agent-a1.jsonl" j1 claude-opus 1 0 0 1
    subagent agent-a2 issue-judge
    message "$work/session/subagents/agent-a2.jsonl" j2 claude-opus 1 0 0 1
    subagent agent-a3 code-explorer
    message "$work/session/subagents/agent-a3.jsonl" e1 claude-sonnet 2 0 0 0
    run_hook "$session"
    assert_success
    assert_line --regexp '^main +claude-opus +1 +10 +100 +1000 +20 +1110 +335$'
    assert_line --regexp '^issue-judge +claude-opus +2 +2 +0 +0 +2 +1 +12$'
    assert_line --regexp '^code-explorer +claude-sonnet +1 +2 +0 +0 +0 +2 +2$'
    assert_line --regexp '^total +4 +14 +100 +1000 +22 +1110 +349$'
}

@test "rows come heaviest first" {
    message "$session" m1 claude-opus 1 0 0 0
    subagent agent-a1 issue-judge
    message "$work/session/subagents/agent-a1.jsonl" j1 claude-opus 0 0 0 10
    run_hook "$session"
    assert_success
    assert_line --index 1 --regexp '^issue-judge '
    assert_line --index 2 --regexp '^main '
}

@test "a message streamed over several lines counts once, with its final output" {
    message "$session" m1 claude-opus 10 0 0 1
    message "$session" m1 claude-opus 10 0 0 5
    run_hook "$session"
    assert_success
    assert_line --regexp '^main +claude-opus +1 +10 +0 +0 +5 +10 +35$'
}

@test "messages without an id count one by one" {
    local uuid
    for uuid in u1 u2; do
        jq -nc --arg uuid "$uuid" '{type: "assistant", uuid: $uuid, message: {model: "claude-opus",
            usage: {input_tokens: 1}}}' >>"$session"
    done
    run_hook "$session"
    assert_success
    assert_line --regexp '^main +claude-opus +2 +2 '
}

@test "a synthetic message and a line without usage are left out" {
    message "$session" m1 '<synthetic>' 0 0 0 0
    echo '{"type": "user", "message": {"content": "hello"}}' >>"$session"
    message "$session" m2 claude-opus 1 0 0 0
    run_hook "$session"
    assert_success
    refute_output --partial synthetic
    assert_line --regexp '^main +claude-opus +1 '
}

@test "a subagent without its meta file counts as agent" {
    message "$session" m1 claude-opus 1 0 0 0
    message "$work/session/subagents/agent-a1.jsonl" a1 claude-opus 1 0 0 0
    run_hook "$session"
    assert_success
    assert_line --regexp '^agent +claude-opus +1 '
}

@test "a transcript path that starts with a dash, as the project folders of Claude Code do, is a file" {
    mkdir -p "$work/-project/session/subagents"
    message "$work/-project/session.jsonl" m1 claude-opus 1 0 0 0
    subagent agent-a1 issue-judge
    mv "$work/session/subagents/agent-a1.meta.json" "$work/-project/session/subagents/"
    message "$work/-project/session/subagents/agent-a1.jsonl" j1 claude-opus 1 0 0 0
    cd "$work"
    run_hook -project/session.jsonl
    assert_success
    assert_line --regexp '^main +claude-opus +1 '
    assert_line --regexp '^issue-judge +claude-opus +1 '
}

@test "a missing transcript prints the usage" {
    run_hook "$work/none.jsonl"
    assert_failure
    assert_stderr --partial "usage: session-tokens"
}
