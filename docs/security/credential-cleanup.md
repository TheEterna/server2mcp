# Credential Cleanup — full writeup

> **This page documents the discovery, scope, and remediation path for
> the 2026-08-05 privacy audit findings.** The high-risk
> remediation (`git filter-repo` + force-push) is **not run
> automatically** — it requires explicit operator action. See
> `scripts/clean-credentials.sh`.

## What leaked (2026-08-05 audit)

| # | Severity | What | Where | Mitigation |
|---|----------|------|-------|------------|
| 1 | 🔴 **CRITICAL** | MySQL prod credentials: `jdbc:mysql://62.234.92.252:3306/yizi` + `root:hfy` | `server2mcp-test/src/main/resources/application.yml` in git history | Scrub via filter-repo + **rotate the password NOW** |
| 2 | 🟡 medium | Personal QQ email `3168134942@qq.com` as git author | every commit pre-audit | `git filter-repo --email-callback` |
| 3 | 🟢 low | Fake test API key `sk-12345678` | demo only | no action — doesn't leak real secrets |
| 4 | 🟢 low | Internal `@author han` tags in Java | throughout the codebase | no action — author = project lead, intentional |

**The demo app already switched to H2 in commit `fe988b6` (2026-08-03)**
— running code is safe — but **git history is forever** until scrubbed.

## Why this is risky to automate

- `git filter-repo` rewrites every commit hash in the repository.
- Open PRs and forks become unreconcilable.
- Collaborators with local clones must re-clone.
- The leaked MySQL password **must be rotated** at the host regardless
  of how we scrub history — anyone who cloned before `fe988b6` has it.

The script (`scripts/clean-credentials.sh`) does **everything except
the force-push**, so an operator reviews the diff before publishing.

## Step-by-step: how to run the cleanup

### 0. Pre-flight

- Announce the maintenance window in your team channel.
- Block new PRs / merges during the window.
- Confirm you have admin access to the MySQL host to rotate the password.

### 1. Verify the leak

```bash
cd api2mcp4j
git log --all -S"62.234.92.252" --oneline
git log --all -S"hfy" --oneline
```

You should see commit(s) referencing the test app's `application.yml`.
Note the commit hashes — these are the ones that will be rewritten.

### 2. Rotate the leaked MySQL password

**Do this BEFORE scrubbing history.** The scrub only protects future
clones; existing clones already have the password.

```bash
# SSH to the MySQL host (or via your DBA panel):
mysql -u root -p'hfy' -h 62.234.92.252
ALTER USER 'root'@'%' IDENTIFIED BY 'NEW-STRONG-PASSWORD';
FLUSH PRIVILEGES;
```

### 3. Run the cleanup script

```bash
./scripts/clean-credentials.sh
```

The script will:

- Verify the leak is still in history
- Ask for explicit `yes` confirmation
- Install `git-filter-repo` if missing
- Replace each leak pattern with `REDACTED-LEAK-CHECK-DOCS-LOGS`
- Repack and gc the repository
- Print the new HEAD hash

It will **NOT** force-push. The next step is yours.

### 4. Verify the scrub

```bash
git log --all -S"62.234.92.252" --oneline
git log --all -S"hfy" --oneline
# Both should be empty.
```

If anything still matches, you can re-run the script — it's
idempotent.

### 5. Force-push (destructive, intentional)

```bash
git remote add origin git@github.com:TheEterna/api2mcp4j.git   # if needed
git push --force-with-lease origin master
```

`--force-with-lease` (not `--force`) is preferred — it refuses the push
if the remote moved since you last fetched, protecting you from
clobbering someone else's commit.

### 6. Aftermath

- Tell every collaborator to re-clone (their commit hashes are dead).
- Close any open PRs; re-open them against the rewritten history.
- Open a work-log entry in `docs/logs/` recording the cleanup date
  and the password-rotation date.

## What we changed in the codebase to prevent this from happening again

| File | Change |
|------|--------|
| `.gitignore` (existing) | already excludes `application-local.yml`, `*.env` — kept |
| `server2mcp-test/src/main/resources/application.yml` (commit `fe988b6`) | demo switched to H2; production datasource is now injected via env var only |
| `scripts/clean-credentials.sh` (this PR) | one-shot scrub script with explicit confirmation |
| `docs/security/credential-cleanup.md` (this page) | runbook so the next operator doesn't have to reverse-engineer the process |

## Future hardening (out of scope for this PR)

- **Pre-commit hook** that scans staged diffs against a deny-list of
  patterns (`jdbc:mysql://`, `password:`, `private_key`, etc.). BFG's
  `--blobs-then-sizes` and `git-secrets` (https://github.com/awslabs/git-secrets)
  cover this.
- **CI guard**: a GitHub Action that runs `git log -p` on every PR and
  fails if a known-leak pattern appears in any diff (history + new
  commits). Catches regressions at PR time.
- **Secret scanner** on the running app: pull `secrets-manager` from
  your cloud (AWS SM, Vault, Aliyun KMS) and inject as env at boot.
  Eliminates the file-based datasource entirely.
