# git commits in a sample repository whose hooks folder holds pre-commit, run by $HOOK_SHELL, next to a fake
# lint-kotlin and a fake lint-frontend that turn "ugly" into "pretty".

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
    stub "$work/hooks/lint-frontend" <<STUB
printf '%s\n' "\$@" >>"$work/frontend-runs"
[ -f "$work/lint-fails" ] && exit 1
for f; do
    sed 's/ugly/pretty/' "\$f" >"\$f.formatted"
    mv "\$f.formatted" "\$f"
done
STUB

    git init -q "$repo"
    git -C "$repo" config core.hooksPath "$work/hooks"
    printf '{\n  "scripts": {\n    "build": "nuxt build"\n  },\n  "dependencies": {\n    "vue": "^3.5.0"\n  }\n}\n' >"$repo/front/package.json"
    cp "$repo/front/package.json" "$repo/package.json"
    printf "catalog:\n  '@nuxt/kit': 4.6.4\n  vue: 3.5.0\n" >"$repo/pnpm-workspace.yaml"
    echo "lockfileVersion: '9.0'" >"$repo/pnpm-lock.yaml"
    git -C "$repo" add .
    git -C "$repo" commit -q --no-verify -m init
}

# Stages <file> with <content>, and commits it without the hook first when <committed> is given.
staged_file() {
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

# Bumps every version of <file> that starts with <from> to <to>, and stages it.
bump() {
    sed "s/$2/$3/" "$repo/$1" >"$repo/$1.bumped"
    mv "$repo/$1.bumped" "$repo/$1"
    git -C "$repo" add -- "$1"
}

bump_vue() {
    bump "$1" '\^3.5.0' '^3.6.0'
}

stage_lock() {
    echo "lockfileVersion: '9.1'" >"$repo/pnpm-lock.yaml" && git -C "$repo" add pnpm-lock.yaml
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
    assert_output "pre-commit: front/package.json changes a dependency but pnpm-lock.yaml is not staged. Run pnpm install, then stage both"
}

@test "a dependency bump without the lock file, at the root, is refused" {
    bump_vue package.json
    commit
    assert_failure
}

@test "a dependency bump with the lock file is accepted" {
    bump_vue front/package.json
    stage_lock
    commit
    assert_success
}

@test "a catalog bump without the lock file is refused" {
    bump pnpm-workspace.yaml '3.5.0' '3.6.0'
    commit
    assert_failure
    assert_output "pre-commit: pnpm-workspace.yaml changes a dependency but pnpm-lock.yaml is not staged. Run pnpm install, then stage both"
}

@test "a catalog bump of a scoped package without the lock file is refused" {
    bump pnpm-workspace.yaml '4.6.4' '4.7.0'
    commit
    assert_failure
}

@test "a catalog bump with the lock file is accepted" {
    bump pnpm-workspace.yaml '3.5.0' '3.6.0'
    stage_lock
    commit
    assert_success
}

@test "a dependency moved to the catalog without the lock file is refused" {
    bump front/package.json '"\^3.5.0"' '"catalog:"'
    commit
    assert_failure
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
    staged_file Cellar.kt 'class Cellar(val ugly: String)'
    commit
    assert_success
}

@test "a fully staged Kotlin file is committed formatted" {
    staged_file Cellar.kt 'class Cellar(val ugly: String)'
    commit
    run committed Cellar.kt
    assert_output "class Cellar(val pretty: String)"
}

@test "a partly staged Kotlin file is committed as staged" {
    staged_file Bottle.kt 'class Bottle(val ugly: Int)'
    echo '// draft' >>"$repo/Bottle.kt"
    commit
    run committed Bottle.kt
    assert_output "class Bottle(val ugly: Int)"
}

@test "a partly staged Kotlin file is formatted on disk" {
    staged_file Bottle.kt 'class Bottle(val ugly: Int)'
    echo '// draft' >>"$repo/Bottle.kt"
    commit
    run cat "$repo/Bottle.kt"
    assert_output "class Bottle(val pretty: Int)
// draft"
}

@test "a partly staged Kotlin file is said to be left unstaged" {
    staged_file Bottle.kt 'class Bottle(val ugly: Int)'
    echo '// draft' >>"$repo/Bottle.kt"
    commit
    assert_output "pre-commit: Bottle.kt reformatted, left unstaged (it had other unstaged changes)"
}

@test "a deleted Kotlin file is accepted" {
    staged_file Cellar.kt 'class Cellar(val pretty: String)' committed
    git -C "$repo" rm -q Cellar.kt
    commit
    assert_success
}

@test "a deleted Kotlin file runs no lint" {
    staged_file Cellar.kt 'class Cellar(val pretty: String)' committed
    git -C "$repo" rm -q Cellar.kt
    commit
    assert [ ! -e "$work/lint-runs" ]
}

@test "a renamed Kotlin file is committed formatted" {
    staged_file Bottle.kt 'class Bottle(val ugly: Int)' committed
    git -C "$repo" mv Bottle.kt Wine.kt
    commit
    run committed Wine.kt
    assert_output "class Bottle(val pretty: Int)"
}

@test "a Kotlin file with a space in its name is committed formatted" {
    staged_file "My Rack.kt" 'class Rack(val ugly: Int)'
    commit
    run committed "My Rack.kt"
    assert_output "class Rack(val pretty: Int)"
}

@test "a Kotlin file that lint-kotlin fails on is refused" {
    touch "$work/lint-fails"
    staged_file Cork.kt 'class Cork(val ugly: Int)'
    commit
    assert_failure
}

@test "a fully staged TypeScript file is committed formatted" {
    staged_file cellar.ts 'export const ugly = 1'
    commit
    run committed cellar.ts
    assert_output "export const pretty = 1"
}

@test "a fully staged Vue file is committed formatted" {
    staged_file CellarCard.vue '<template>ugly</template>'
    commit
    run committed CellarCard.vue
    assert_output "<template>pretty</template>"
}

@test "a fully staged CSS file is committed formatted" {
    staged_file app.css '.ugly {}'
    commit
    run committed app.css
    assert_output ".pretty {}"
}

@test "lint-frontend is given the staged TypeScript, Vue and CSS files only" {
    staged_file cellar.ts 'export const pretty = 1'
    staged_file CellarCard.vue '<template>pretty</template>'
    staged_file app.css '.pretty {}'
    staged_file Cellar.kt 'class Cellar(val pretty: String)'
    staged_file notes.md 'pretty'
    commit
    run sort "$work/frontend-runs"
    assert_output "CellarCard.vue
app.css
cellar.ts"
}

@test "a partly staged TypeScript file is committed as staged" {
    staged_file bottle.ts 'export const ugly = 1'
    echo '// draft' >>"$repo/bottle.ts"
    commit
    run committed bottle.ts
    assert_output "export const ugly = 1"
}

@test "a commit without TypeScript, Vue nor CSS files runs no frontend lint" {
    staged_file Cellar.kt 'class Cellar(val pretty: String)'
    commit
    assert [ ! -e "$work/frontend-runs" ]
}

@test "a TypeScript file that lint-frontend fails on is refused" {
    touch "$work/lint-fails"
    staged_file cork.ts 'export const ugly = 1'
    commit
    assert_failure
}
