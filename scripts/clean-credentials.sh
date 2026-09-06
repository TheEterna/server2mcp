#!/usr/bin/env bash
#
# clean-credentials.sh — remove leaked MySQL credentials from git history.
#
# ⚠️  HIGH-RISK OPERATION — rewrites every commit ever pushed.
#     After running this you MUST force-push, and every clone/fork
#     must reset. Coordinate with collaborators first.
#
# What this does:
#   1. Verifies the leak is in history (sanity check, prints the commit).
#   2. Runs `git filter-repo` to remove the credential strings.
#   3. Re-packs the repo and prints the new head hash.
#   4. STOPS — does NOT force-push. You review + push manually.
#
# Usage:
#   cd api2mcp4j
#   ./scripts/clean-credentials.sh
#
# Requirements: Python 3.7+ and `git-filter-repo` (`pip install git-filter-repo`).
#
# References:
#   - docs/security/credential-cleanup.md (full writeup)
#   - docs/logs/2026-08-05_ceo_隐私审计.md (original audit)
#
set -euo pipefail

# --- Patterns to scrub (extend as more leaks surface) ---
PATTERNS=(
  "62.234.92.252"          # leaked MySQL host
  "jdbc:mysql://"          # any MySQL JDBC URL
  "root:hfy"               # leaked user:pass combo
  "hfy"                    # leaked password (broad — review collisions)
  "3168134942@qq.com"      # leaked email (lower priority)
)

# --- Sanity check: is the leak actually in history? ---
echo "==> Step 1/4 — searching history for the leaked strings…"
LEAK_FOUND=0
for pat in "${PATTERNS[@]}"; do
  if git log --all -S"$pat" --oneline -- . ':!docs/logs/' ':!docs/security/' 2>/dev/null | head -1 | grep -q .; then
    echo "    ✓ found '$pat' in:"
    git log --all -S"$pat" --oneline -- . ':!docs/logs/' ':!docs/security/' | head -3
    LEAK_FOUND=1
  fi
done

if [ "$LEAK_FOUND" -eq 0 ]; then
  echo "==> No leaks found in current history. Did you already clean up? Exiting."
  exit 0
fi

# --- Confirm with the operator ---
cat <<'WARN'

================================================================
  ⚠️  THIS WILL REWRITE GIT HISTORY
================================================================
  • Every commit hash will change.
  • Every open PR must be rebased or closed.
  • After this script you must `git push --force-with-lease` to
    publish. (The script does NOT push for you — by design.)
  • Make sure the leak is not in your working tree, too:
      grep -r "62.234.92.252" .
      grep -r "hfy" .
WARN

read -r -p "Continue? Type 'yes' to proceed: " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  echo "Aborted. No changes made."
  exit 0
fi

# --- Install git-filter-repo if missing ---
if ! command -v git-filter-repo >/dev/null 2>&1; then
  echo "==> Step 2/4 — installing git-filter-repo (pip)…"
  pip install --user git-filter-repo
fi

# --- Build the --replace-text args (one per pattern, paired replacement) ---
REPLACE_ARGS=()
for pat in "${PATTERNS[@]}"; do
  REPLACE_ARGS+=("--replace-text" "/dev/stdin" <<<"$pat==>REDACTED-LEAK-CHECK-DOCS-LOGS")
done

# --- Run filter-repo ---
echo "==> Step 3/4 — running git filter-repo…"
# filter-repo only takes one --replace-text per call, so we loop.
for pat in "${PATTERNS[@]}"; do
  echo "    scrubbing: $pat"
  REPLACEMENT="${pat}==>REDACTED-LEAK-CHECK-DOCS-LOGS"
  git filter-repo --force --replace-text <(echo "$REPLACEMENT")
done

# --- Repack and verify ---
echo "==> Step 4/4 — repacking + verifying…"
git reflog expire --expire=now --all
git gc --prune=now --aggressive

NEW_HEAD=$(git rev-parse HEAD)
echo ""
echo "================================================================"
echo "  ✓ Done. New HEAD: $NEW_HEAD"
echo ""
echo "  NEXT STEPS (you must do these manually):"
echo "    1. Review the diff:  git log --stat"
echo "    2. Verify no leak remains:"
for pat in "${PATTERNS[@]}"; do
  echo "         git log --all -S'$pat' --oneline | head"
done
echo "    3. Force-push (DESTRUCTIVE — coordinate with collaborators first):"
echo "         git remote add origin git@github.com:TheEterna/api2mcp4j.git   # if needed"
echo "         git push --force-with-lease origin master"
echo ""
echo "  Other things to do after force-push:"
echo "    • Rotate the leaked MySQL password (the 'hfy' one is now public)."
echo "    • Tell collaborators to re-clone (their commit hashes are dead)."
echo "    • Close and re-open any open PRs."
echo "================================================================"
