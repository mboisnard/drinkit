# lint-frontend runs from the root of a clone, through a fake pnpm, on a workspace with one linted package.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/git/lint-frontend
    mkdir -p "$work/repo/front/app/pages" "$work/repo/deployment"
    touch "$work/repo/front/eslint.config.ts"
    cd "$work/repo" || return
    stub pnpm <<STUB
printf '%s\n' "\$@" >>"$work/pnpm-args"
[ ! -f "$work/findings" ] && exit 0
echo "\$PWD/front/app/cellar.ts"
echo "  1:28  error  Missing semicolon  @stylistic/semi"
exit 1
STUB
}

pnpm_args() {
    if [ -f "$work/pnpm-args" ]; then cat "$work/pnpm-args"; else echo "no run"; fi
}

@test "ESLint fixes the files of a linted package" {
    run_hook front/app/cellar.ts "front/app/pages/my cellar.vue"
    run pnpm_args
    assert_output "exec
eslint
--fix
--max-warnings
0
--
front/app/cellar.ts
front/app/pages/my cellar.vue"
}

@test "Stylelint fixes the Vue and CSS files of a package that configures it" {
    touch front/.stylelintrc.json
    run_hook front/app/app.css "front/app/pages/my cellar.vue" front/app/cellar.ts
    run pnpm_args
    assert_output "exec
eslint
--fix
--max-warnings
0
--
front/app/pages/my cellar.vue
front/app/cellar.ts
exec
stylelint
--fix
./front/app/app.css
./front/app/pages/my cellar.vue"
}

@test "a package without a Stylelint config runs no Stylelint" {
    run_hook front/app/app.css
    assert_success
    assert_equal "$(pnpm_args)" "no run"
}

@test "a file named with a leading dash reads as a file to Stylelint" {
    touch front/.stylelintrc.json
    run_hook front/-theme.css
    run pnpm_args
    assert_output --partial "./front/-theme.css"
}

@test "a Stylelint finding fails it" {
    touch front/.stylelintrc.json "$work/findings"
    run_hook front/app/app.css
    assert_failure
}

@test "an ESLint finding still lets Stylelint fix the file" {
    touch front/.stylelintrc.json "$work/findings"
    run_hook front/app/CellarCard.vue
    assert_failure
    run pnpm_args
    assert_output --partial "stylelint"
}

@test "a file outside every linted package is left out" {
    run_hook deployment/seed.ts front/app/cellar.ts
    run pnpm_args
    assert_output --partial "front/app/cellar.ts"
    refute_output --partial "deployment/seed.ts"
}

@test "an absolute path inside a linted package is linted" {
    run_hook "$work/repo/front/app/cellar.ts"
    run pnpm_args
    assert_output --partial "$work/repo/front/app/cellar.ts"
}

@test "only files outside every linted package run no ESLint" {
    run_hook deployment/seed.ts
    assert_success
    assert_equal "$(pnpm_args)" "no run"
}

@test "a clean run passes" {
    run_hook front/app/cellar.ts
    assert_success
}

@test "an ESLint finding fails it" {
    touch "$work/findings"
    run_hook front/app/cellar.ts
    assert_failure
}

@test "the findings are shown" {
    touch "$work/findings"
    run_hook front/app/cellar.ts
    assert_output --partial "1:28  error  Missing semicolon  @stylistic/semi"
}
