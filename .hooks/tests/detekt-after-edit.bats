# detekt-after-edit receives Claude Code's PostToolUse input of an Edit or a Write, and runs a fake lint-kotlin.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/detekt-after-edit
    repo=$work/repo
    mkdir -p "$repo/.hooks/git" "$repo/src" "$work/no-lint/src" "$work/outside"
    git init -q "$repo"
    git init -q "$work/no-lint"
    stub "$repo/.hooks/git/lint-kotlin" <<STUB
echo run >>"$work/runs"
if [ -f "$work/findings" ]; then
    echo "> Task :drinkit-domain:detekt"
    echo "w: file://\$PWD/src/Money.kt:10:18 Non-public primary constructor is exposed via the generated 'copy()' method"
    echo "e: \$PWD/src/Cellar.kt:12:5 Missing newline before the closing brace [Wrapping]"
    echo "BUILD FAILED"
    exit 1
fi
if [ -f "$work/crash" ]; then
    echo "FAILURE: Build failed with an exception."
    echo "* What went wrong:"
    echo "Could not resolve all files for configuration ':detekt'."
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
    if [ -f "$work/runs" ]; then grep -c run "$work/runs"; else echo 0; fi
}

@test "a Markdown file is left alone" {
    edited "$repo/README.md"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a clean Kotlin file passes" {
    edited "$repo/src/Cellar.kt"
    assert_success
    assert_equal "$(lint_runs)" 1
}

@test "a Kotlin file written whole is linted" {
    edited "$repo/src/Bottle.kt" Write
    assert_success
    assert_equal "$(lint_runs)" 1
}

@test "a Gradle script is linted" {
    edited "$repo/build.gradle.kts"
    assert_success
    assert_equal "$(lint_runs)" 1
}

@test "a Kotlin file outside a repository is left alone" {
    edited "$work/outside/Script.kt"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a repository without lint-kotlin is left alone" {
    edited "$work/no-lint/src/Cellar.kt"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "a detekt failure blocks" {
    touch "$work/findings"
    edited "$repo/src/Cellar.kt"
    assert_failure 2
    assert_equal "$(lint_runs)" 1
}

@test "the finding is shown to Claude" {
    touch "$work/findings"
    edited "$repo/src/Cellar.kt"
    assert_stderr --partial "Cellar.kt:12:5 Missing newline before the closing brace [Wrapping]"
}

@test "compiler warnings are left out" {
    touch "$work/findings"
    edited "$repo/src/Cellar.kt"
    refute_stderr --partial "Money.kt"
}

@test "a build failure without findings blocks" {
    touch "$work/crash"
    edited "$repo/src/Cellar.kt"
    assert_failure 2
    assert_equal "$(lint_runs)" 1
}

@test "the end of the output is shown when there is no finding" {
    touch "$work/crash"
    edited "$repo/src/Cellar.kt"
    assert_stderr --partial "Could not resolve all files for configuration ':detekt'."
}

@test "a pull request under review runs no build" {
    git -C "$repo" switch -q -c pr-451
    edited "$repo/src/Cellar.kt"
    assert_success
    assert_equal "$(lint_runs)" 0
}

@test "nothing is ever printed to stdout" {
    edited "$repo/src/Cellar.kt"
    refute_output
    touch "$work/findings"
    edited "$repo/src/Cellar.kt"
    refute_output
    edited "$work/outside/Script.kt"
    refute_output
}
