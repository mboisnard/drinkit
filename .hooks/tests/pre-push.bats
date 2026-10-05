# pre-push reads the refs git is about to push on stdin, one line each: <local ref> <sha> <remote ref> <sha>.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/git/pre-push
    sha=1111111111111111111111111111111111111111
    zero=0000000000000000000000000000000000000000
}

push() {
    run_hook origin git@github.com:mboisnard/drinkit.git <<<"$1"
}

@test "a push to master is refused" {
    push "refs/heads/master $sha refs/heads/master $zero"
    assert_failure
    assert_stderr "pre-push: master only changes through a pull request, push a branch and open one"
}

@test "a push of another branch onto master is refused" {
    push "refs/heads/382-implement-issue $sha refs/heads/master $sha"
    assert_failure
}

@test "a deletion of master is refused" {
    push "(delete) $zero refs/heads/master $sha"
    assert_failure
}

@test "master among several refs is refused" {
    push "refs/heads/382-implement-issue $sha refs/heads/382-implement-issue $zero
refs/heads/master $sha refs/heads/master $sha"
    assert_failure
}

@test "a branch push is accepted" {
    push "refs/heads/382-implement-issue $sha refs/heads/382-implement-issue $zero"
    assert_success
}

@test "a branch deletion is accepted" {
    push "(delete) $zero refs/heads/382-implement-issue $sha"
    assert_success
}

@test "a branch whose name contains master is accepted" {
    push "refs/heads/master-fix $sha refs/heads/master-fix $zero"
    assert_success
}

@test "a tag named master is accepted" {
    push "refs/tags/master $sha refs/tags/master $zero"
    assert_success
}

@test "nothing to push is accepted" {
    push ""
    assert_success
}

@test "git runs it from .hooks/git and refuses a push to master" {
    git init -q -b master "$work/repo"
    git init -q --bare -b master "$work/remote.git"
    git -C "$work/repo" commit -q --allow-empty -m init
    git -C "$work/repo" config core.hooksPath "$HOOKS/git"

    run git -C "$work/repo" push "$work/remote.git" master

    assert_failure
    assert_output --partial "pre-push: master only changes through a pull request"
    run git -C "$work/remote.git" rev-parse --verify -q master
    assert_failure
}
