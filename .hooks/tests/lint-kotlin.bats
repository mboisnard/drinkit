# lint-kotlin runs from the root of a clone, through the Gradle wrapper there, here a fake one.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/git/lint-kotlin
    mkdir "$work/repo"
    cd "$work/repo" || return
    stub "$work/repo/gradlew" <<STUB
echo "\$*" >"$work/gradle-args"
echo "> Task :drinkit-domain:detekt"
[ ! -f "$work/findings" ] && exit 0
echo "e: \$PWD/src/Cellar.kt:12:5 Missing newline before the closing brace [Wrapping]"
exit 1
STUB
}

@test "detekt runs on every module and corrects what it can" {
    run_hook
    run cat "$work/gradle-args"
    assert_output "detektAll -Pdetekt.autoCorrect=true --console=plain"
}

@test "a clean run passes" {
    run_hook
    assert_success
}

@test "a detekt finding fails it" {
    touch "$work/findings"
    run_hook
    assert_failure
}

@test "the findings are shown, not only their count" {
    touch "$work/findings"
    run_hook
    assert_output --partial "src/Cellar.kt:12:5 Missing newline before the closing brace [Wrapping]"
}
