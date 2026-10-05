# shellcheck shell=sh
# Sourced by the Claude Code hooks that make sure git runs the hooks of .hooks/git, which keep master safe.

# Prints why git runs no hook of .hooks/git in the clone of <folder>, and nothing when it does.
git_hooks_off() {
    if [ "$(git -C "$1" config core.hooksPath 2>/dev/null)" != .hooks/git ]; then
        echo "core.hooksPath is not .hooks/git. Run git -C $1 config core.hooksPath .hooks/git"
    elif ! git_hooks_root=$(git -C "$1" rev-parse --show-toplevel 2>/dev/null) ||
        [ ! -x "$git_hooks_root/.hooks/git/pre-push" ]; then
        echo ".hooks/git/pre-push is missing or not executable. Rebase the branch on origin/master"
    fi
}
