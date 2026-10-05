# The harness files hold together: each hook has its suite, each settings command runs a hook with a timeout, each skill
# glob matches a tracked file, each link under .claude/ leads to a file. Each check runs on this clone and a broken one.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    repository=$(cd "$HOOKS/.." && pwd)
    broken=$work/broken
    git init -q "$broken"
    mkdir -p "$broken/.hooks/git" "$broken/.hooks/claude" "$broken/.hooks/tests" "$broken/.claude/skills/sample"
}

# Each hook of <root>/.hooks/git and <root>/.hooks/claude without its .hooks/tests/<hook>.bats.
hooks_without_suite() {
    local hook
    for hook in "$1"/.hooks/git/* "$1"/.hooks/claude/*; do
        [ -f "$hook" ] || continue
        [ -f "$1/.hooks/tests/${hook##*/}.bats" ] || echo "${hook#"$1"/}"
    done
}

# Each command of <root>/.claude/settings.json that runs no executable of .hooks/claude, or has no timeout.
settings_commands_out_of_place() {
    jq -r '.hooks[][].hooks[] | select(.type == "command") | [.command, (.timeout // "")] | @tsv' \
        "$1/.claude/settings.json" | while IFS=$'\t' read -r command timeout; do
        local hook=${command#'"$CLAUDE_PROJECT_DIR"/'}
        case $hook in
            .hooks/claude/*) [ -x "$1/$hook" ] || echo "$command: no such hook" ;;
            *) echo "$command: not a hook of .hooks/claude" ;;
        esac
        [ -n "$timeout" ] || echo "$command: no timeout"
    done
}

# Each paths: glob of a skill under <root>/.claude/skills that matches no tracked file.
skill_globs_matching_nothing() {
    local skill glob
    for skill in "$1"/.claude/skills/*/SKILL.md; do
        [ -f "$skill" ] || continue
        awk '
            NR == 1 && $0 == "---" { front = 1; next }
            front && $0 == "---" { exit }
            front && /^paths:/ { paths = 1; next }
            front && paths && /^  - / { sub(/^  - /, ""); gsub(/^"|"$/, ""); print; next }
            front && paths { paths = 0 }
        ' "$skill" | while IFS= read -r glob; do
            [ -n "$(git -C "$1" ls-files -- ":(glob)$glob")" ] || echo "${skill#"$1"/}: $glob"
        done
    done
}

# Each relative link of a Markdown file under <root>/.claude that leads to no file. Web links and anchors are left out.
relative_links_leading_nowhere() {
    local file target
    git -C "$1" ls-files -- ':(glob).claude/**/*.md' | while IFS= read -r file; do
        grep -o '](\([^)]*\))' "$1/$file" | sed 's/^](//; s/)$//; s/ .*//; s/#.*//' | while IFS= read -r target; do
            case $target in
                '' | http://* | https://* | mailto:*) continue ;;
            esac
            [ -e "$(dirname "$1/$file")/$target" ] || echo "$file: $target"
        done
    done
}

@test "every hook has its suite" {
    run hooks_without_suite "$repository"
    assert_success
    refute_output
}

@test "a hook without its suite is reported" {
    touch "$broken/.hooks/git/pre-push" "$broken/.hooks/claude/session-context" "$broken/.hooks/tests/pre-push.bats"
    run hooks_without_suite "$broken"
    assert_output ".hooks/claude/session-context"
}

@test "every command of the Claude Code settings runs a hook of .hooks/claude with a timeout" {
    run settings_commands_out_of_place "$repository"
    assert_success
    refute_output
}

@test "a settings command running no hook, or no timeout, is reported" {
    stub "$broken/.hooks/claude/session-context" </dev/null
    cat >"$broken/.claude/settings.json" <<'JSON'
{"hooks": {
  "SessionStart": [{"hooks": [
    {"type": "command", "command": "\"$CLAUDE_PROJECT_DIR\"/.hooks/claude/session-context", "timeout": 5},
    {"type": "command", "command": "\"$CLAUDE_PROJECT_DIR\"/.hooks/claude/moved-away", "timeout": 5},
    {"type": "command", "command": "\"$CLAUDE_PROJECT_DIR\"/.claude/hooks/session-context", "timeout": 5}
  ]}],
  "Stop": [{"hooks": [{"type": "command", "command": "\"$CLAUDE_PROJECT_DIR\"/.hooks/claude/session-context"}]}]
}}
JSON
    run settings_commands_out_of_place "$broken"
    assert_output '"$CLAUDE_PROJECT_DIR"/.hooks/claude/moved-away: no such hook
"$CLAUDE_PROJECT_DIR"/.claude/hooks/session-context: not a hook of .hooks/claude
"$CLAUDE_PROJECT_DIR"/.hooks/claude/session-context: no timeout'
}

@test "every paths: glob of a skill matches a tracked file" {
    run skill_globs_matching_nothing "$repository"
    assert_success
    refute_output
}

@test "a paths: glob matching no tracked file is reported" {
    mkdir -p "$broken/.hooks/git"
    touch "$broken/.hooks/git/pre-push"
    git -C "$broken" add .hooks
    cat >"$broken/.claude/skills/sample/SKILL.md" <<'SKILL'
---
name: sample
description: A skill whose globs point at moved folders
paths:
  - ".hooks/**"
  - ".githooks/**"
  - "drinkit/*/src/**/*.kt"
---

# Sample
SKILL
    run skill_globs_matching_nothing "$broken"
    assert_output '.claude/skills/sample/SKILL.md: .githooks/**
.claude/skills/sample/SKILL.md: drinkit/*/src/**/*.kt'
}

@test "every relative link under .claude/ leads to a file" {
    run relative_links_leading_nowhere "$repository"
    assert_success
    refute_output
}

@test "a relative link leading to no file is reported, a web link or an anchor is not" {
    touch "$broken/.claude/skills/sample/examples.md"
    cat >"$broken/.claude/skills/sample/SKILL.md" <<'SKILL'
# Sample

The commands are in [examples.md](examples.md), the prompt in [the example](examples.md#judge-prompt).
The reference is [reference.md](reference.md), the rules in [the guide](../../../docs/guide.md "Guide").
See [the docs](https://code.claude.com/docs/en/hooks) and [below](#sample).
SKILL
    git -C "$broken" add .claude
    run relative_links_leading_nowhere "$broken"
    assert_output '.claude/skills/sample/SKILL.md: reference.md
.claude/skills/sample/SKILL.md: ../../../docs/guide.md'
}
