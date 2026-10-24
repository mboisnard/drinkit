# guard-github-protections receives Claude Code's PreToolUse input of a Bash command, run from a clone whose git hooks
# are on, from one where they are off, or from outside any clone. Each line of the table below is a test of its own.

setup() {
    # shellcheck source=helper.bash
    source "$BATS_TEST_DIRNAME/helper.bash"
    sandbox
    HOOK=$HOOKS/claude/guard-github-protections
    hooks_on=$HOME/on
    hooks_off=$HOME/off
    outside=$HOME/outside
    mkdir "$outside"
    git init -q "$hooks_on"
    git -C "$hooks_on" config core.hooksPath .hooks/git
    mkdir -p "$hooks_on/.hooks/git"
    stub "$hooks_on/.hooks/git/pre-push" </dev/null
    git init -q "$hooks_off"
}

# Claude is about to run <command> from <folder>: on, off, outside, hooks for the .hooks/git of on, or a path.
runs() {
    local folder
    case $2 in
        off) folder=$hooks_off ;;
        outside) folder=$outside ;;
        hooks) folder=$hooks_on/.hooks/git ;;
        /*) folder=$2 ;;
        *) folder=$hooks_on ;;
    esac
    run_hook_on pre-tool-use-bash '.tool_input.command = $command | .cwd = $cwd' --arg command "$1" --arg cwd "$folder"
}

# Case <number>: the guard allows, asks or denies <command>, run from <folder>. @on@ and @off@ in the command stand
# for those clones.
hook_decides() {
    local expected=$2 command=$3
    command=${command//@on@/$hooks_on}
    command=${command//@off@/$hooks_off}
    runs "$command" "${4:-on}"
    assert_success
    if [ "$expected" = allow ]; then
        refute_output
    else
        assert_equal "$(decision)" "$expected"
    fi
}

decision() {
    jq -r .hookSpecificOutput.permissionDecision <<<"$output"
}

reason() {
    jq -r .hookSpecificOutput.permissionDecisionReason <<<"$output"
}

# Registers a test of hook_decides, named after the decision and the command on one line. bats evaluates the name,
# and takes a test whose arguments start like another's for a duplicate: the case number keeps them apart.
cases=0
decides() {
    local name="$1  $2"
    [ -z "${3:-}" ] || name="$name, from $3"
    name=${name//$'\n'/ }
    name=${name//\\/\\\\}
    name=${name//\"/\\\"}
    name=${name//\$/\\\$}
    name=${name//\`/\\\`}
    cases=$((cases + 1))
    bats_test_function --description "$name" -- hook_decides "$cases" "$@"
}

decides deny 'gh api --method DELETE repos/mboisnard/drinkit/rulesets/0'
decides deny 'gh api -X DELETE repos/mboisnard/drinkit/rulesets/24070575'
decides deny 'gh api -X "PATCH" repos/mboisnard/drinkit/rulesets/24070575 -f enforcement=disabled'
decides deny 'gh api -X delete repos/mboisnard/drinkit/rulesets/24070575'
decides deny 'gh api --method=PUT repos/mboisnard/drinkit/rulesets/24070575 --input .github/rulesets/master.json'
decides deny 'gh api repos/mboisnard/drinkit/rulesets/24070575 --input .github/rulesets/master.json'
decides deny 'gh api repos/{owner}/{repo}/rulesets -f name=open -f enforcement=disabled'
decides deny 'gh api -X PUT repos/mboisnard/drinkit/branches/master/protection --input protection.json'
decides deny 'gh api -X DELETE /repos/mboisnard/drinkit/branches/master/protection/enforce_admins'
decides deny "gh api graphql -f query='mutation { deleteRepositoryRuleset(input: {repositoryRulesetId: \"x\"}) { clientMutationId } }'"
decides deny "gh api graphql -f query='mutation { updateBranchProtectionRule(input: {}) { clientMutationId } }'"
decides deny 'gh api repos/mboisnard/drinkit/secret-scanning/push-protection-bypasses -f reason=false_positive -f placeholder_id=x'
# shellcheck disable=SC2016
decides deny 'curl -X DELETE -H "Authorization: Bearer $(gh auth token)" https://api.github.com/repos/mboisnard/drinkit/rulesets/1'
decides deny 'curl -d @master.json https://api.github.com/repos/mboisnard/drinkit/rulesets'
decides deny 'git status && gh api -X PATCH repos/mboisnard/drinkit/rulesets/1 -f enforcement=disabled'
decides deny 'cd docs
gh api --method DELETE repos/mboisnard/drinkit/rulesets/1'
decides deny 'gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  /repos/OWNER/REPO/rulesets/RULESET_ID \
  -f "enforcement=disabled"'
decides deny 'gh api \
  --method POST \
  /repos/OWNER/REPO/secret-scanning/push-protection-bypasses \
  -f "reason=used_in_tests" -f "placeholder_id=x"'
decides deny 'curl -L \
  -X PUT \
  https://api.github.com/repos/OWNER/REPO/rulesets/1 \
  -d @master.json'
decides deny 'curl -sSX DELETE https://api.github.com/repos/mboisnard/drinkit/rulesets/1'
decides deny 'curl -sSd @body.json https://api.github.com/repos/mboisnard/drinkit/secret-scanning/push-protection-bypasses'
decides deny 'curl -d@master.json https://api.github.com/repos/mboisnard/drinkit/rulesets'
decides deny 'curl -T master.json https://api.github.com/repos/mboisnard/drinkit/rulesets/1'
decides deny 'curl -g -d @master.json https://api.github.com/repos/mboisnard/drinkit/rulesets'
decides deny 'gh api repos/mboisnard/drinkit/rulesets -fname=open -fenforcement=disabled'

decides allow 'gh api repos/mboisnard/drinkit/rulesets'
decides allow "gh api repos/mboisnard/drinkit/rulesets/24070575 --jq '.rules[].type'"
decides allow 'gh api -X GET repos/mboisnard/drinkit/rulesets -f per_page=100'
decides allow 'gh api repos/mboisnard/drinkit/branches/master/protection'
decides allow 'gh api repos/mboisnard/drinkit/secret-scanning/alerts'
decides allow 'gh ruleset list'
decides allow 'gh ruleset view 24070575'
decides allow 'gh api -X POST repos/mboisnard/drinkit/pulls -f title=x -f head=y -f base=master'
decides allow "gh api --method PATCH repos/mboisnard/drinkit -f 'security_and_analysis[secret_scanning][status]=enabled'"
decides allow 'gh api --method PUT repos/mboisnard/drinkit/private-vulnerability-reporting'
decides allow 'git add .github/rulesets/master.json && git commit -F message.txt'
decides allow 'gh api repos/mboisnard/drinkit/rulesets --jq length && gh issue comment 1 -F body.md'
decides allow 'jq . .github/rulesets/master.json'
decides allow 'gh api \
  /repos/OWNER/REPO/rulesets \
  --paginate'
decides allow 'curl -sSL https://api.github.com/repos/mboisnard/drinkit/rulesets'
decides allow 'curl -sS -D headers.txt https://api.github.com/repos/mboisnard/drinkit/rulesets'
decides allow 'curl -G -d per_page=100 https://api.github.com/repos/mboisnard/drinkit/rulesets'

decides deny 'gh pr merge 450 --rebase'
decides deny 'gh pr merge'
decides deny 'env gh pr merge 450'
decides deny 'GH_REPO=mboisnard/drinkit gh pr merge 450'
decides deny "bash -c 'gh pr merge 450'"
decides deny 'gh "pr" merge 450'
decides deny 'echo 450 | xargs -n1 gh pr merge'
decides deny 'git status && gh pr merge 450 --auto'
decides deny 'gh -R mboisnard/drinkit pr merge 450'
decides deny 'gh --repo mboisnard/drinkit pr merge 450'
decides deny 'gh pr --repo=mboisnard/drinkit merge 450'
decides deny '/opt/homebrew/bin/gh pr merge 450'
decides deny '\gh pr merge 450'
decides deny 'if true; then gh pr merge 450; fi'
# shellcheck disable=SC2016
decides deny 'for i in 450; do gh pr merge $i; done'
decides deny 'timeout 60 gh pr merge 450'
decides deny 'true & gh pr merge 450'
decides deny "gh api graphql -f query='mutation { enqueuePullRequest(input: {pullRequestId: \"x\"}) { clientMutationId } }'"
decides deny 'gh api --method PUT repos/mboisnard/drinkit/pulls/450/merge'
decides deny 'gh api -X PUT repos/{owner}/{repo}/pulls/450/merge -f merge_method=rebase'
decides deny 'gh api repos/mboisnard/drinkit/pulls/450/merge -f merge_method=rebase'
# shellcheck disable=SC2016
decides deny 'curl -X PUT -H "Authorization: Bearer $(gh auth token)" https://api.github.com/repos/mboisnard/drinkit/pulls/450/merge'
decides deny "gh api graphql -f query='mutation { mergePullRequest(input: {pullRequestId: \"x\"}) { clientMutationId } }'"
decides deny "gh api graphql -f query='mutation { enablePullRequestAutoMerge(input: {pullRequestId: \"x\"}) { clientMutationId } }'"
decides deny 'gh api graphql -F query=@merge.graphql'
decides deny 'gh api graphql --input query.json'
decides deny "gh alias set land 'pr merge --rebase'"
decides deny 'gh alias import aliases.yml'
decides deny 'git push --no-verify origin HEAD:master'
decides deny 'git push origin 382-implement-issue --no-verify'
decides deny 'git -c core.hooksPath=/dev/null push origin HEAD:master'
decides deny 'git -C . -c core.hookspath= push'
decides deny 'git config --unset core.hooksPath'
decides deny 'git config core.hooksPath /dev/null'
decides deny 'git config core.hooksPath .githooks'
decides deny 'GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=core.hooksPath GIT_CONFIG_VALUE_0=/dev/null git push'
decides deny 'export GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=core.hooksPath GIT_CONFIG_VALUE_0=/dev/null; git push origin HEAD:master'
decides deny "GIT_CONFIG_PARAMETERS=\"'core.hooksPath'='/dev/null'\" git push origin HEAD:master"
decides deny 'git push --no-verif origin HEAD:master'
decides deny 'git config --global core.hooksPath /dev/null'
decides deny 'git config --global --unset core.hooksPath'
decides deny 'git config -f .git/config core.hooksPath /dev/null'
decides deny 'git config set core.hooksPath /dev/null'
decides deny 'git config --global core.hooksPath .hooks/git'
decides deny 'git config --system core.hooksPath .hooks/git'
decides deny 'git config set --global core.hooksPath .hooks/git'
decides deny 'git -c core.hooksPath=/dev/null commit -m x'
decides deny 'git rebase --exec "git config core.hooksPath /dev/null" HEAD~1'
decides deny 'git submodule foreach "git config core.hooksPath /dev/null"'
decides deny 'git fetch --upload-pack="git config core.hooksPath /dev/null; git-upload-pack" .'
decides deny 'git grep -O"git config core.hooksPath /dev/null;" foo'
decides deny 'git grep -nO"git config core.hooksPath /dev/null;" foo'
decides deny 'git grep --open-files="git config core.hooksPath /dev/null;" foo'
decides deny 'git -c "a.b=x log" config core.hooksPath /dev/null'
decides deny 'git -c a.b=x\ log config core.hooksPath /dev/null'
decides deny 'git --namespace "a log" config core.hooksPath /dev/null'
decides deny 'git -C "sub log" config core.hooksPath /dev/null'
decides deny 'git --shallow-file log config core.hooksPath /dev/null'
decides deny 'git -c "a.b=x commit" -c core.hooksPath=/dev/null push origin HEAD:master'
decides deny 'git push origin HEAD:master' off
decides deny 'git push origin HEAD:master' outside
decides deny 'git -C ../off push -u origin HEAD'
decides deny "git -C @off@ push -u origin HEAD"
decides deny 'git -C .. -C off push -u origin HEAD'
decides deny "git --git-dir=@off@/.git push origin HEAD"
decides deny 'cd ../off && git push -u origin HEAD'
decides deny "cd @off@; git push -u origin HEAD"
decides deny 'cd ~/off && git push -u origin HEAD'
decides deny 'cd .. && git -C off push -u origin HEAD'
decides deny "git push origin HEAD && git -C @off@ push origin HEAD"

decides allow 'gh pr view 450'
decides allow 'gh pr create --base master --title x --body-file body.md'
decides allow 'gh pr checks 450 --watch'
decides allow 'gh api repos/mboisnard/drinkit/pulls/450/merge'
decides allow "gh api graphql -f query='query { viewer { login } }'"
decides allow 'gh alias list'
decides allow 'git push -u origin HEAD'
decides allow 'git push --force-with-lease --force-if-includes'
decides allow 'git -C ../on push --force-with-lease origin 382-implement-issue' off
decides allow "git -C @on@ push -u origin HEAD" outside
decides allow 'cd ../on && git push -u origin HEAD' off
decides allow 'cd ~/on && git push -u origin HEAD' outside
decides allow 'cd ../off && cd ../on && git push -u origin HEAD'
decides allow 'git config core.hooksPath'
decides allow 'git config core.hooksPath .hooks/git'
decides allow 'git config --local core.hooksPath .hooks/git'
decides allow 'git config set core.hooksPath .hooks/git'
decides allow 'git config set --local core.hooksPath .hooks/git'
decides allow 'git config --worktree core.hooksPath .hooks/git'
decides allow 'git config --get core.hooksPath'
decides allow 'git config get core.hooksPath'
decides allow 'git config --global core.hooksPath'
decides allow 'git config --system core.hooksPath'
decides allow 'git config --show-origin core.hooksPath'
decides allow 'git config --show-scope --get core.hooksPath'
decides allow 'git -C /path/to/worktree config core.hooksPath'
# shellcheck disable=SC2016
decides allow 'test "$(git config core.hooksPath)" = .hooks/git'
# shellcheck disable=SC2016
decides allow '[ "$(git config core.hooksPath)" = .hooks/git ] || echo off'
decides allow './gradlew build 2>&1 | tail -n 20'
decides allow 'git commit -m "Refuse gh pr merge from agent sessions"'
decides allow 'git commit -m "Refuse merges; gh pr merge is for the maintainer"'
decides allow 'git commit -m "Never chain git status && gh pr merge 450"'
decides allow 'git log --grep "gh pr merge"'
decides allow 'git commit -m "Mention core.hooksPath"'
decides allow 'git commit -m "[HARNESS] Keep core.hooksPath out of the global and system configuration (#456)"'
decides allow 'git log --grep core.hooksPath'
decides allow 'git log -S core.hooksPath --oneline'
decides allow 'grep -rn "core.hooksPath" AGENTS.md'
decides allow 'git status' off
decides allow 'git status' outside
decides allow 'gh pr merge --help'
decides allow 'gh pr merge -h'

decides deny 'gh pr merge --help && gh pr merge 450'
decides deny 'gh pr merge 450 -h --rebase'
decides deny "bash -c 'git status; gh pr merge 450'"
# shellcheck disable=SC2016
decides deny 'echo "$(gh pr merge 450)"'
decides deny 'gh pr merge 450' outside

decides allow "git commit -F - <<'EOF'
Refuse merges from agent sessions

- \`gh pr merge\` stays with the maintainer
- git push --no-verify is refused too
EOF"
# shellcheck disable=SC2016
decides allow 'git commit -m "$(cat <<'"'"'EOF'"'"'
Refuse merges from agent sessions

- `gh pr merge` stays with the maintainer; so do rulesets
EOF
)"'
# shellcheck disable=SC2016
decides allow 'gh pr create --title x --body "$(cat <<EOF
Never run \`git config core.hooksPath /dev/null\` or gh pr merge
EOF
)"'
decides allow "cat >notes.md <<-EOF
	gh pr merge 450
	EOF"
decides deny "gh pr merge 450 <<'EOF'
y
EOF"
decides deny "cat >notes.md <<'EOF'
merge notes
EOF
gh pr merge 450"
decides deny "bash <<'EOF'
gh pr merge 450
EOF"
decides deny "cat <<'EOF' | sh
gh pr merge 450
EOF"
# shellcheck disable=SC2016
decides allow "cat >notes.md <<'EOF'
\$(gh pr merge 450)
EOF"
decides allow "cat >notes.md <<EOF
gh pr merge 450 stays with the maintainer
EOF"
# shellcheck disable=SC2016
decides deny 'cat >notes.md <<EOF
$(gh pr merge 450)
EOF'
decides deny "cat >notes.md <<EOF
merged: \`gh pr merge 450\`
EOF"
# shellcheck disable=SC2016
decides deny 'gh api graphql -f query="$(cat <<EOF
mutation { mergePullRequest(input: {pullRequestId: "x"}) { clientMutationId } }
EOF
)"'

decides ask "sed -i '' 's/deny/allow/' .hooks/claude/guard-github-protections"
decides ask 'sed -i.bak 1d .hooks/git/pre-push'
decides ask 'sed --in-place 1d .hooks/git/pre-push'
decides ask "sed -n 'w .hooks/git/pre-push' /dev/null"
decides ask "perl -pi -e 's/deny/allow/' .hooks/claude/guard-github-protections"
decides ask 'cp /tmp/pre-push .hooks/git/pre-push'
decides ask 'mv .hooks/git/pre-push .hooks/git/pre-push.off'
decides ask 'rm -f .hooks/git/pre-push'
decides ask 'chmod -x .hooks/git/pre-push'
decides ask 'ln -sf /usr/bin/true .hooks/git/pre-push'
decides ask 'install -m 755 /tmp/pre-push .hooks/git/'
decides ask 'rsync -a /tmp/hooks/ .hooks/'
decides ask 'touch .hooks/claude/new-hook'
decides ask 'echo exit 0 > .hooks/git/pre-push'
decides ask "printf 'exit 0\n' >>.hooks/claude/guard-github-protections"
decides ask 'cat .hooks/git/pre-push > .hooks/git/pre-push.bak'
decides ask 'echo exit 0 | tee .hooks/git/pre-push'
decides ask "cat > .hooks/git/pre-push <<'EOF'
#!/bin/sh
exit 0
EOF"
decides ask 'git checkout HEAD~1 -- .hooks/'
decides ask 'git restore --source=HEAD~1 .hooks/git/pre-push'
decides ask 'git diff --output=.hooks/git/pre-push'
decides ask "python3 -c \"open('.hooks/git/pre-push', 'w').write('exit 0')\""
decides ask "bash -c 'echo exit 0 > .hooks/git/pre-push'"
decides ask "echo 'rm .hooks/git/pre-push' | sh"
decides ask 'cd .hooks/git && echo exit 0 > pre-push'
decides ask 'rm pre-push' hooks
decides ask 'ls .hooks/git | xargs rm'
# shellcheck disable=SC2016
decides ask 'f=.hooks/git/pre-push; rm $f'
decides ask 'find .hooks -name pre-push -delete'
decides ask "find .hooks -name '*.bats' -exec sed -i '' s/a/b/ {} +"
decides ask 'npm install --prefix .hooks left-pad'
# shellcheck disable=SC2016
decides ask 'docker run --rm -v "$PWD:/repo" alpine rm /repo/.hooks/git/pre-push'
decides ask 'git status && rm .hooks/git/pre-push'
decides ask "python3 - <<'EOF'
open('.hooks/git/pre-push', 'w').write('exit 0')
EOF"
decides ask "node - <<'EOF'
require('fs').writeFileSync('.hooks/git/pre-push', 'exit 0')
EOF"
decides ask 'tree -o .hooks/claude/guard-github-protections /nonexistent'
decides ask 'sort -o .hooks/git/pre-push /tmp/pre-push'
decides ask 'rg --pre ./rewrite deny .hooks/claude'
# shellcheck disable=SC2016
decides ask 'docker run --rm -v$PWD:/repo alpine rm /repo/.hooks/git/pre-push'
# shellcheck disable=SC2016
decides ask 'docker run --rm -itv "$PWD:/repo" alpine rm /repo/.hooks/git/pre-push'
decides ask 'docker run --rm --volumes-from dev alpine rm /repo/.hooks/git/pre-push'
decides ask 'echo \>&rm .hooks/git/pre-push'
decides ask '.hooks/../../../../bin/rm .hooks/git/pre-push'
decides ask "git config alias.x '!rm .hooks/git/pre-push'"
decides ask 'git config -f .hooks/git/pre-push core.x y'
decides ask 'git -c core.fsmonitor=./rewrite status -- .hooks'
decides ask "git grep -O'rm .hooks/git/pre-push' deny"
decides ask "GIT_EDITOR='tee .hooks/git/pre-push' git commit --allow-empty"
decides ask 'GIT_EXTERNAL_DIFF=./rewrite git diff -- .hooks'
decides ask "git fetch --upload-pack='cp /dev/null .hooks/git/pre-push; git-upload-pack' ."
# shellcheck disable=SC2016
decides ask 'f=.hooks/claude/new-hook; echo exit 0 > "$f"'
# shellcheck disable=SC2016
decides ask 'cd .hooks/git && echo exit 0 > "$PWD/pre-push"'
decides deny 'rm .hooks/git/pre-push && gh pr merge 450'
decides deny 'gh pr merge 450; rm .hooks/git/pre-push'
decides deny 'rm .hooks/git/pre-push && git push origin HEAD' off
decides deny 'echo \>&gh pr merge 450'
decides deny 'echo \>|git config core.hooksPath /dev/null'
decides deny 'cat \<&gh pr merge 450'

decides allow 'cat .hooks/git/pre-push'
decides allow 'head -n 40 .hooks/claude/guard-github-protections'
decides allow "sed -n '1,40p' .hooks/claude/guard-github-protections"
decides allow 'grep -rn hookspath .hooks'
decides allow 'rg -n deny .hooks/claude'
decides allow 'ls -la .hooks .hooks/*'
decides allow "find .hooks -name '*.bats' | xargs wc -l"
decides allow 'jq .scripts .hooks/package.json'
decides allow 'diff .hooks/git/pre-push ../off/.hooks/git/pre-push'
decides allow 'git diff origin/master -- .hooks/'
decides allow 'git log --oneline -5 -- .hooks'
decides allow 'git show HEAD:.hooks/git/pre-push'
decides allow 'git ls-files .hooks'
decides allow 'git add .hooks/claude/guard-github-protections && git commit -m "Ask before a write under .hooks/"'
decides allow 'npm cit --prefix .hooks'
decides allow 'npm cit --prefix .hooks 2>&1 | tail -n 30'
decides allow 'cd .hooks && HOOK_SHELL=dash npx bats tests/guard-github-protections.bats'
decides allow "docker run --rm -v \"\$PWD:/mnt:ro\" -w /mnt --entrypoint sh \"\$(grep -o 'koalaman/shellcheck[^ ]*' .github/workflows/ci-hooks.yml)\" -c 'shellcheck .hooks/git/* .hooks/claude/* .hooks/lib/*.sh .hooks/tests/*.bats .hooks/tests/*.bash'"
decides allow 'sh -n .hooks/claude/guard-github-protections'
decides allow '.hooks/claude/session-context < input.json'
# shellcheck disable=SC2016
decides allow '"$CLAUDE_PROJECT_DIR"/.hooks/claude/guard-github-protections < input.json'
decides allow 'cat .hooks/git/pre-push > pre-push.txt'
decides allow 'grep -c deny .hooks/claude/guard-github-protections 2>/dev/null'
decides allow 'gh pr create --title "[HARNESS] Ask before a Bash command writes under .hooks (#456)" --body-file body.md'
decides allow "git commit -F - <<'EOF'
Ask before a Bash command writes under .hooks/

- rm .hooks/git/pre-push now waits for the maintainer
EOF"
decides allow 'cat .github/workflows/ci-hooks.yml'
decides allow 'cat pre-push' hooks
decides allow 'npx bats ../tests/pre-push.bats' hooks
decides allow "cat > notes.md <<'EOF'
rm .hooks/git/pre-push now waits for the maintainer
EOF"
decides allow 'wc -l .hooks/claude/* | sort -n'
decides allow 'npm run test --prefix .hooks'
decides allow 'git -C .hooks log --oneline -3'


@test "allow  without jq, with a warning on stderr" {
    only_tools "$work/no-jq" awk cat git
    hook_path=$work/no-jq runs 'gh pr merge 450'
    assert_success
    refute_output
    assert_stderr --partial "jq not found"
}

@test "allow  a push without git installed" {
    only_tools "$work/no-git" awk cat jq
    hook_path=$work/no-git runs 'git push origin HEAD' off
    assert_success
    refute_output
}

# A clone whose core.hooksPath is .hooks/git, with <pre-push>: missing or not-executable.
clone_with_pre_push() {
    git init -q "$work/clone"
    git -C "$work/clone" config core.hooksPath .hooks/git
    mkdir -p "$work/clone/.hooks/git"
    [ "$1" = missing ] || touch "$work/clone/.hooks/git/pre-push"
}

@test "deny  a push from a clone whose .hooks/git holds no pre-push" {
    clone_with_pre_push missing
    runs 'git push -u origin HEAD' "$work/clone"
    assert_equal "$(decision)" deny
}

@test "deny  a push from a clone whose pre-push is not executable" {
    clone_with_pre_push not-executable
    runs 'git push -u origin HEAD' "$work/clone"
    assert_equal "$(decision)" deny
}

@test "a push without pre-push is told to rebase the branch" {
    clone_with_pre_push missing
    runs 'git push -u origin HEAD' "$work/clone"
    assert_equal "$(reason)" "guard-github-protections: the git hooks do not run in $work/clone: .hooks/git/pre-push is missing or not executable. Rebase the branch on origin/master, then push again: git push -u origin HEAD"
}

@test "a write under .hooks/ tells the maintainer why it waits" {
    runs 'rm .hooks/git/pre-push'
    assert_equal "$(reason)" "guard-github-protections: this command may write under .hooks/, whose hooks keep master safe. Approve it only if you asked for this change: rm .hooks/git/pre-push"
}

@test "deny  a write under .hooks/ in bypassPermissions mode, where no one approves it" {
    run_hook_on pre-tool-use-bash '.tool_input.command = $command | .cwd = $cwd | .permission_mode = "bypassPermissions"' \
        --arg command 'rm .hooks/git/pre-push' --arg cwd "$hooks_on"
    assert_equal "$(decision)" deny
    assert_equal "$(reason)" "guard-github-protections: no one can approve a write under .hooks/ in bypassPermissions mode. Change a hook with the Edit tool, which asks the maintainer: rm .hooks/git/pre-push"
}

@test "a core.hooksPath set for every repository is told to set it in the clone" {
    runs 'git config --global core.hooksPath .hooks/git'
    assert_equal "$(reason)" "guard-github-protections: core.hooksPath is set in each clone, never globally, where it would run the hooks of every repository. Run git config core.hooksPath .hooks/git in the clone: git config --global core.hooksPath .hooks/git"
}

@test "a push with the git hooks off is told the command that turns them on" {
    runs 'git push -u origin HEAD' off
    assert_equal "$(reason)" "guard-github-protections: the git hooks do not run in $hooks_off: core.hooksPath is not .hooks/git. Run git -C $hooks_off config core.hooksPath .hooks/git, then push again: git push -u origin HEAD"
}
