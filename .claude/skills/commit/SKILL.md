---
name: commit
description: Pull latest changes, then create a conventional commit. Use this instead of committing directly to avoid merge conflicts with CI pipelines.
---

Before committing, always pull to integrate any remote changes (e.g. automated version bumps from CI):

1. Run `git pull --no-rebase` and resolve any conflicts if they arise.
2. Then follow the standard commit protocol:
   - Run `git status` and `git diff` (staged + unstaged) in parallel with `git log --oneline -5` to understand the changes and follow the repo's commit message style.
   - Stage the relevant files by name (avoid `git add -A` or `git add .`).
   - Write a concise conventional commit message focused on the "why".
   - Commit using a HEREDOC to pass the message, co-authored by Claude.
   - Run `git status` after to confirm success.

$ARGUMENTS
