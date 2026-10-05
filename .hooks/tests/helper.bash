# Sourced by every suite: the assertions of bats-assert, a sandbox cut off from the machine, stubs on PATH, Claude Code
# inputs from fixtures/, and the hook under test run by the shell of $HOOK_SHELL.
# shellcheck disable=SC2034,SC2153 # the suites read HOOKS and set HOOK

bats_require_minimum_version 1.5.0
# Not their load.bash, which spawns a dirname per file: 15 processes in each test, half of its time on macOS
for library in "$BATS_TEST_DIRNAME"/../node_modules/bats-support/src/*.bash "$BATS_TEST_DIRNAME"/../node_modules/bats-assert/src/*.bash; do
    # shellcheck source=/dev/null
    source "$library"
done

HOOKS=$(cd "$BATS_TEST_DIRNAME/.." && pwd)

# sh by default. CI also runs every suite under dash and "bash --posix". By its full path, so that a test can run the
# hook with a PATH that lacks it
read -ra hook_shell <<<"${HOOK_SHELL:-sh}"
hook_shell[0]=$(command -v "${hook_shell[0]}")

# A home, a git configuration and a locale of the test's own, with $work/bin first on PATH. bats removes $work.
sandbox() {
    # Resolved, since git prints /private/var where macOS hands out /var
    work=$(cd "$BATS_TEST_TMPDIR" && pwd -P)
    mkdir -p "$work/home" "$work/bin"
    export HOME=$work/home GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1 GIT_CEILING_DIRECTORIES=$work
    export LC_ALL=C TZ=UTC
    export GIT_AUTHOR_NAME=test GIT_AUTHOR_EMAIL=test@example.com
    export GIT_COMMITTER_NAME=test GIT_COMMITTER_EMAIL=test@example.com
    unset CLAUDE_PROJECT_DIR CMUX_SURFACE_ID CMUX_WORKSPACE_ID XDG_CONFIG_HOME
    # Set when the suites run from a git hook, they would point every git command at the clone
    local variable
    while read -r variable; do
        unset "$variable"
    done < <(git rev-parse --local-env-vars)
    PATH=$work/bin:$PATH
}

# Writes the script read on stdin to $work/bin/<name>, or to <path> when it holds a slash, and makes it executable.
stub() {
    local file=$1
    case $file in
        */*) ;;
        *) file=$work/bin/$file ;;
    esac
    {
        echo '#!/bin/sh'
        cat
    } >"$file"
    chmod +x "$file"
}

# Fills <folder> with links to the real <tools>, to run a hook with PATH=<folder> as if nothing else were installed.
only_tools() {
    local folder=$1 tool
    shift
    mkdir -p "$folder"
    for tool; do
        ln -s "$(command -v "$tool")" "$folder/$tool"
    done
}

# Prints fixtures/<name>.json changed by a jq filter: event stop '.cwd = $cwd' --arg cwd "$repo"
event() {
    local name=$1 filter=${2:-.}
    jq "${@:3}" "$filter" "$BATS_TEST_DIRNAME/fixtures/$name.json"
}

# Runs $HOOK under the shell of $HOOK_SHELL, stdout in $output and stderr in $stderr, with PATH=$hook_path when set.
# shellcheck disable=SC2120 # the arguments of the hook, pre-push takes some
run_hook() {
    run --separate-stderr env ${hook_path:+"PATH=$hook_path"} "${hook_shell[@]}" "$HOOK" "$@"
}

# Runs $HOOK with event <name> <filter> [jq options] on its stdin.
run_hook_on() {
    local input
    input=$(event "$@") || return
    # shellcheck disable=SC2119 # the arguments of run_hook_on build the input, not the hook's arguments
    run_hook <<<"$input"
}
