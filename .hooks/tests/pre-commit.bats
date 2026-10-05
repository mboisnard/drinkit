# git commits in a sample repository whose hooks folder holds pre-commit, run by $HOOK_SHELL, next to a fake
# lint-kotlin that turns "ugly" into "pretty".

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    repo=$work/repo
    mkdir -p "$work/hooks" "$repo/front"
    cp "$HOOKS/git/pre-commit" "$work/hooks/pre-commit-under-test"
    stub "$work/hooks/pre-commit" <<STUB
exec ${HOOK_SHELL:-sh} "$work/hooks/pre-commit-under-test" "\$@"
STUB
    stub "$work/hooks/lint-kotlin" <<STUB
echo run >>"$work/lint-runs"
[ -f "$work/lint-fails" ] && exit 1
find . -name '*.kt' -type f | while IFS= read -r f; do
    sed 's/ugly/pretty/' "\$f" >"\$f.formatted"
    mv "\$f.formatted" "\$f"
done
STUB

    git init -q "$repo"
    git -C "$repo" config core.hooksPath "$work/hooks"
    printf '{\n  "scripts": {\n    "build": "nuxt build"\n  },\n  "dependencies": {\n    "vue": "^3.5.0"\n  }\n}\n' >"$repo/front/package.json"
    cp "$repo/front/package.json" "$repo/package.json"
    echo '{}' >"$repo/front/package-lock.json"
    echo '{}' >"$repo/package-lock.json"
    git -C "$repo" add .
    git -C "$repo" commit -q --no-verify -m init
}

# Stages <file> with <content>, and commits it without the hook first when <committed> is given.
kotlin_file() {
    echo "$2" >"$repo/$1"
    git -C "$repo" add -- "$1"
    [ -z "${3:-}" ] || git -C "$repo" commit -q --no-verify -m "$1"
}

commit() {
    run git -C "$repo" commit -q -m change
}

committed() {
    git -C "$repo" show "HEAD:$1"
}

bump_vue() {
    sed 's/\^3.5.0/^3.6.0/' "$repo/$1" >"$repo/$1.bumped"
    mv "$repo/$1.bumped" "$repo/$1"
    git -C "$repo" add -- "$1"
}

@test "a small file is accepted" {
    echo notes >"$repo/notes.md" && git -C "$repo" add notes.md
    commit
    assert_success
}

@test "a file over 5 MB is refused" {
    dd if=/dev/zero of="$repo/big.bin" bs=1048576 count=6 2>/dev/null && git -C "$repo" add big.bin
    commit
    assert_failure
    assert_output "pre-commit: big.bin is over 5 MB, keep large files out of the repository"
}

@test "a file over 5 MB with a non-ASCII name is refused" {
    dd if=/dev/zero of="$repo/café.bin" bs=1048576 count=6 2>/dev/null && git -C "$repo" add café.bin
    commit
    assert_failure
}

@test "a renamed file grown over 5 MB is refused" {
    dd if=/dev/zero of="$repo/data.bin" bs=1048576 count=4 2>/dev/null
    git -C "$repo" add data.bin
    git -C "$repo" commit -q --no-verify -m data
    git -C "$repo" mv data.bin moved.bin
    dd if=/dev/zero bs=1048576 count=3 2>/dev/null >>"$repo/moved.bin" && git -C "$repo" add moved.bin
    commit
    assert_failure
}

@test "a dependency bump without the lock file is refused" {
    bump_vue front/package.json
    commit
    assert_failure
    assert_output "pre-commit: front/package.json changes a dependency but front/package-lock.json is not staged. Run npm install, then stage both"
}

@test "a dependency bump without the lock file, at the root, is refused" {
    bump_vue package.json
    commit
    assert_failure
}

@test "a dependency bump with its lock file is accepted" {
    bump_vue front/package.json
    echo '{"lockfileVersion": 3}' >"$repo/front/package-lock.json" && git -C "$repo" add front
    commit
    assert_success
}

@test "a script change without the lock file is accepted" {
    sed 's/nuxt build/nuxt build --prerender/' "$repo/front/package.json" >"$repo/front/package.json.new"
    mv "$repo/front/package.json.new" "$repo/front/package.json" && git -C "$repo" add front/package.json
    commit
    assert_success
}

@test "a deleted package.json is accepted" {
    git -C "$repo" rm -q front/package.json
    commit
    assert_success
}

@test "a fully staged Kotlin file is accepted" {
    kotlin_file Cellar.kt 'class Cellar(val ugly: String)'
    commit
    assert_success
}

@test "a fully staged Kotlin file is committed formatted" {
    kotlin_file Cellar.kt 'class Cellar(val ugly: String)'
    commit
    run committed Cellar.kt
    assert_output "class Cellar(val pretty: String)"
}

@test "a partly staged Kotlin file is committed as staged" {
    kotlin_file Bottle.kt 'class Bottle(val ugly: Int)'
    echo '// draft' >>"$repo/Bottle.kt"
    commit
    run committed Bottle.kt
    assert_output "class Bottle(val ugly: Int)"
}

@test "a partly staged Kotlin file is formatted on disk" {
    kotlin_file Bottle.kt 'class Bottle(val ugly: Int)'
    echo '// draft' >>"$repo/Bottle.kt"
    commit
    run cat "$repo/Bottle.kt"
    assert_output "class Bottle(val pretty: Int)
// draft"
}

@test "a partly staged Kotlin file is said to be left unstaged" {
    kotlin_file Bottle.kt 'class Bottle(val ugly: Int)'
    echo '// draft' >>"$repo/Bottle.kt"
    commit
    assert_output "pre-commit: Bottle.kt reformatted, left unstaged (it had other unstaged changes)"
}

@test "a deleted Kotlin file is accepted" {
    kotlin_file Cellar.kt 'class Cellar(val pretty: String)' committed
    git -C "$repo" rm -q Cellar.kt
    commit
    assert_success
}

@test "a deleted Kotlin file runs no lint" {
    kotlin_file Cellar.kt 'class Cellar(val pretty: String)' committed
    git -C "$repo" rm -q Cellar.kt
    commit
    assert [ ! -e "$work/lint-runs" ]
}

@test "a renamed Kotlin file is committed formatted" {
    kotlin_file Bottle.kt 'class Bottle(val ugly: Int)' committed
    git -C "$repo" mv Bottle.kt Wine.kt
    commit
    run committed Wine.kt
    assert_output "class Bottle(val pretty: Int)"
}

@test "a Kotlin file with a space in its name is committed formatted" {
    kotlin_file "My Rack.kt" 'class Rack(val ugly: Int)'
    commit
    run committed "My Rack.kt"
    assert_output "class Rack(val pretty: Int)"
}

@test "a Kotlin file that lint-kotlin fails on is refused" {
    touch "$work/lint-fails"
    kotlin_file Cork.kt 'class Cork(val ugly: Int)'
    commit
    assert_failure
}
