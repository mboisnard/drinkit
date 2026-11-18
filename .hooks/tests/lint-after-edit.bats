# lint-after-edit receives Claude Code's PostToolUse input of an Edit or a Write, and runs a fake lint-frontend.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/lint-after-edit
    repo=$work/repo
    mkdir -p "$repo/.hooks/git" "$repo/node_modules" "$repo/app" "$work/no-lint/app" "$work/outside"
    git init -q "$repo"
    git init -q "$work/no-lint"
    stub pnpm <<<'exit 0'
    stub "$repo/.hooks/git/lint-frontend" <<STUB
echo "\$*" >>"$work/runs"
if [ -f "$work/findings" ]; then
    echo
    echo "\$PWD/app/cellar.ts"
    echo "  1:28  error  Missing semicolon  @stylistic/semi"
    echo
    echo "✖ 1 problem (1 error, 0 warnings)"
    exit 1
fi
STUB
}

# Claude Code edited <file>, with <tool>, Edit by default.
edited() {
    run_hook_on post-tool-use-write '.tool_name = $tool | .tool_input.file_path = $file' \
        --arg file "$1" --arg tool "${2:-Edit}"
}

lint_runs() {
    if [ -f "$work/runs" ]; then wc -l <"$work/runs" | tr -d ' '; else echo 0; fi
}

@test "a Markdown file is left alone" {
    edited "$repo/README.md"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a clean TypeScript file passes" {
    edited "$repo/app/cellar.ts"
    assert_success
    assert_equal "$(lint_runs)" 1
}

@test "a Vue file written whole is linted" {
    edited "$repo/app/CellarCard.vue" Write
    assert_success
    assert_equal "$(lint_runs)" 1
}

@test "a CSS file is linted" {
    edited "$repo/app/app.css"
    assert_success
    assert_equal "$(lint_runs)" 1
}

@test "lint-frontend is given the edited file" {
    edited "$repo/app/cellar.ts"
    run cat "$work/runs"
    assert_output "$repo/app/cellar.ts"
}

@test "a TypeScript file outside a repository is left alone" {
    edited "$work/outside/script.ts"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a repository without lint-frontend is left alone" {
    edited "$work/no-lint/app/cellar.ts"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a repository whose dependencies are not installed is left alone" {
    rmdir "$repo/node_modules"
    edited "$repo/app/cellar.ts"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "without pnpm installed, nothing is linted" {
    only_tools "$work/tools" dirname git jq
    hook_path=$work/tools edited "$repo/app/cellar.ts"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a lint failure blocks" {
    touch "$work/findings"
    edited "$repo/app/cellar.ts"
    assert_failure 2
}

@test "the finding is shown to Claude" {
    touch "$work/findings"
    edited "$repo/app/cellar.ts"
    assert_stderr --partial "1:28  error  Missing semicolon  @stylistic/semi"
}

@test "a pull request under review runs no lint" {
    git -C "$repo" switch -q -c pr-451
    edited "$repo/app/cellar.ts"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "nothing is ever printed to stdout" {
    edited "$repo/app/cellar.ts"
    refute_output
    touch "$work/findings"
    edited "$repo/app/cellar.ts"
    refute_output
}
