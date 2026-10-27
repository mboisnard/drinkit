# shellcheck shell=sh
# Sourced by the Claude Code hooks that make sure git runs the hooks of .hooks/git, which keep master safe.

# Succeeds when git runs the hooks of .hooks/git in the clone of <folder>, otherwise prints why and fails.
git_hooks_on() {
    if [ "$(git -C "$1" config core.hooksPath 2>/dev/null)" != .hooks/git ]; then
        echo "core.hooksPath is not .hooks/git. Run git -C $1 config core.hooksPath .hooks/git"
        return 1
    fi
    git_hooks_root=$(git -C "$1" rev-parse --show-toplevel 2>/dev/null) && [ -x "$git_hooks_root/.hooks/git/pre-push" ] ||
        { echo ".hooks/git/pre-push is missing or not executable. Rebase the branch on origin/master"; return 1; }
}
