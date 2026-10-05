# compile-at-stop receives Claude Code's SessionStart and Stop inputs in a sample repository, with a fake gradlew and a
# fake npm that record their runs.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/compile-at-stop
    repo=$work/repo
    mkdir -p "$work/outside" "$repo/src" "$repo/front/src" "$repo/front/node_modules" "$repo/admin/node_modules" "$repo/docs"
    git init -q "$repo"
    echo 'node_modules/' >"$repo/.gitignore"
    echo 'class Cellar' >"$repo/src/Cellar.kt"
    echo 'export const cellar = 1' >"$repo/front/src/cellar.ts"
    echo '{"scripts": {"typecheck": "vue-tsc"}}' >"$repo/front/package.json"
    echo '{"scripts": {"typecheck": "vue-tsc"}}' >"$repo/admin/package.json"
    echo '{"scripts": {"build": "vitepress build"}}' >"$repo/docs/package.json"
    stub "$repo/gradlew" <<STUB
echo "gradle \$(basename "\$PWD")" >>"$work/runs"
if [ -f "$work/kotlin-broken" ]; then
    echo "e: file://\$PWD/src/Cellar.kt:1:7 Unresolved reference 'Bottle'."
    exit 1
fi
if [ -f "$work/gradle-crash" ]; then
    echo "FAILURE: Build failed with an exception."
    echo "Could not resolve all files for configuration ':compileClasspath'."
    exit 1
fi
if [ -f "$work/gradle-slow" ]; then
    echo \$\$ >"$work/gradle.pid"
    exec sleep 30
fi
STUB
    stub npm <<STUB
cat >/dev/null
echo "npm \$(basename "\$PWD") \$*" >>"$work/runs"
if [ -f "$work/typescript-broken" ]; then
    echo "src/cellar.ts(1,14): error TS2322: Type 'string' is not assignable to type 'number'."
    exit 2
fi
STUB
    git -C "$repo" add .
    git -C "$repo" commit -q -m init
    session_starts
}

session_starts() {
    run_hook_on session-start '.source = $source | .cwd = $cwd' --arg source "${1:-startup}" --arg cwd "$repo"
}

# A turn ends with Claude's cwd in <folder>, the repository by default. $runs then lists what it ran.
turn_ends() {
    rm -f "$work/runs"
    run_hook_on stop '.cwd = $cwd' --arg cwd "${1:-$repo}"
    runs=
    if [ -f "$work/runs" ]; then runs=$(tr '\n' ',' <"$work/runs"); fi
}

# The fake gradlew and npm ran <runs>, comma-separated, and the end of the turn is blocked or not.
checked() {
    assert_equal "$runs" "$1"
    if [ "$2" = blocked ]; then
        assert_equal "$(jq -r .decision <<<"$output")" block
    else
        refute_output
    fi
}

cellar() {
    echo "class Cellar(val $1: String)" >"$repo/src/Cellar.kt"
}

typescript() {
    echo "$1" >"$repo/front/src/cellar.ts"
}

every_typecheck="npm admin run --silent typecheck,npm front run --silent typecheck,"

@test "a new session prints nothing, which would become context" {
    session_starts
    refute_output
}

@test "a new session records the sources without checking" {
    session_starts
    assert [ ! -e "$work/runs" ]
}

@test "a new session takes the changes of a previous one as checked" {
    cellar label
    session_starts
    turn_ends
    checked "" passed
}

@test "a turn without source changes checks nothing" {
    turn_ends
    checked "" passed
}

@test "a turn that changed only Markdown checks nothing" {
    echo 'notes' >"$repo/README.md"
    turn_ends
    checked "" passed
}

@test "a Kotlin change compiles once" {
    cellar name
    turn_ends
    checked "gradle repo," passed
}

@test "a compile failure blocks the end of the turn" {
    touch "$work/kotlin-broken"
    cellar bottle
    turn_ends
    checked "gradle repo," blocked
}

@test "a compile failure is shown to Claude" {
    touch "$work/kotlin-broken"
    cellar bottle
    turn_ends
    assert_output --partial "Unresolved reference 'Bottle'"
}

@test "the same state is not checked twice, so it cannot loop" {
    touch "$work/kotlin-broken"
    cellar bottle
    turn_ends
    turn_ends
    checked "" passed
}

@test "a frontend change runs the type check of every app that has one" {
    typescript 'export const cellar = 2'
    turn_ends
    checked "$every_typecheck" passed
}

@test "a type check failure blocks the end of the turn" {
    touch "$work/typescript-broken"
    typescript "export const cellar: number = 'two'"
    turn_ends
    checked "$every_typecheck" blocked
}

@test "a type check failure is shown to Claude" {
    touch "$work/typescript-broken"
    typescript "export const cellar: number = 'two'"
    turn_ends
    assert_output --partial "error TS2322"
}

@test "an app without its dependencies installed is not type checked" {
    rm -rf "$repo/front/node_modules"
    typescript "export const cellar: number = 'three'"
    turn_ends
    checked "npm admin run --silent typecheck," passed
}

@test "without npm installed, nothing is type checked" {
    only_tools "$work/tools" cat cksum dirname git grep head jq mkdir tail xargs
    typescript "export const cellar = 4"
    hook_path=$work/tools turn_ends
    checked "" passed
}

@test "once npm is installed, the change left unchecked is type checked" {
    only_tools "$work/tools" cat cksum dirname git grep head jq mkdir tail xargs
    typescript "export const cellar = 4"
    hook_path=$work/tools turn_ends
    turn_ends
    checked "$every_typecheck" passed
}

@test "a new untracked Kotlin file counts as a change" {
    echo 'class Bottle' >"$repo/src/Bottle.kt"
    turn_ends
    checked "gradle repo," passed
}

# A change made before a session start of <source> is checked at the end of the next turn.
change_before() {
    echo "class Bottle(val $1: String)" >"$repo/src/Bottle.kt"
    session_starts "$1"
}

@test "a compact session start prints nothing" { change_before compact; refute_output; }
@test "a change made before a compact session start is still checked" { change_before compact; turn_ends; checked "gradle repo," passed; }
@test "a resume session start prints nothing" { change_before resume; refute_output; }
@test "a change made before a resume session start is still checked" { change_before resume; turn_ends; checked "gradle repo," passed; }
@test "a clear session start prints nothing" { change_before clear; refute_output; }
@test "a change made before a clear session start is still checked" { change_before clear; turn_ends; checked "gradle repo," passed; }
@test "a fork session start prints nothing" { change_before fork; refute_output; }
@test "a change made before a fork session start is still checked" { change_before fork; turn_ends; checked "gradle repo," passed; }

@test "a deleted Kotlin file counts as a change" {
    echo 'class Bottle' >"$repo/src/Bottle.kt"
    turn_ends
    rm "$repo/src/Bottle.kt"
    turn_ends
    checked "gradle repo," passed
}

@test "a staged Kotlin change is checked" {
    cellar vintage
    git -C "$repo" add src/Cellar.kt
    turn_ends
    checked "gradle repo," passed
}

@test "a build failure without compiler errors blocks" {
    touch "$work/gradle-crash"
    cellar region
    turn_ends
    checked "gradle repo," blocked
}

@test "a build failure without compiler errors shows the end of the output" {
    touch "$work/gradle-crash"
    cellar region
    turn_ends
    assert_output --partial "Could not resolve all files for configuration ':compileClasspath'."
}

@test "a repository without an executable gradlew compiles nothing" {
    chmod -x "$repo/gradlew"
    cellar country
    turn_ends
    checked "" passed
}

@test "a turn ending outside the repository, without CLAUDE_PROJECT_DIR, checks nothing" {
    cellar owner
    turn_ends "$work/outside"
    checked "" passed
}

@test "a turn ending outside the repository checks the project of CLAUDE_PROJECT_DIR" {
    cellar owner
    export CLAUDE_PROJECT_DIR=$repo
    turn_ends "$work/outside"
    checked "gradle repo," passed
}

@test "a check killed by the hook timeout runs again at the next turn" {
    touch "$work/gradle-slow"
    cellar grape
    event stop '.cwd = $cwd' --arg cwd "$repo" >"$work/stop.json"
    "${hook_shell[@]}" "$HOOK" <"$work/stop.json" >/dev/null 2>&1 3>&- &
    hook_pid=$!
    tries=0
    while [ ! -s "$work/gradle.pid" ] && [ "$tries" -lt 100 ]; do
        sleep 0.1
        tries=$((tries + 1))
    done
    kill "$hook_pid"
    wait "$hook_pid" 2>/dev/null || true
    kill "$(cat "$work/gradle.pid")"
    rm -f "$work/gradle-slow"
    turn_ends
    checked "gradle repo," passed
}

@test "the first turn in a fresh worktree compiles nothing" {
    git -C "$repo" worktree add -q "$work/linked" -b 400-cellar-label
    turn_ends "$work/linked"
    checked "" passed
}

@test "a change in a fresh worktree compiles it" {
    git -C "$repo" worktree add -q "$work/linked" -b 400-cellar-label
    turn_ends "$work/linked"
    echo 'class Cellar(val label: String)' >"$work/linked/src/Cellar.kt"
    turn_ends "$work/linked"
    checked "gradle linked," passed
}

@test "a pull request under review runs no build" {
    git -C "$repo" switch -q -c pr-451
    echo 'class Bottle(val size: Int)' >"$repo/src/Bottle.kt"
    turn_ends
    checked "" passed
}
