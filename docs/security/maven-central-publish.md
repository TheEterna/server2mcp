# Publishing to Maven Central — operator runbook

The pom is now configured for Sonatype OSSRH. This page walks through
the one-time setup, the per-release flow, and the gotchas.

## One-time setup

### 1. Sonatype JIRA account + groupId claim

1. Create an account at https://issues.sonatype.org (JIRA).
2. File a "New Project" issue requesting the `com.ai.plug` groupId.
   You'll need to prove ownership of the domain OR a GitHub repo
   that matches.
3. Wait ~1-3 business days for the JIRA ticket to be resolved.

### 2. GPG signing key

```bash
# 1. Generate (use a real email you control)
gpg --gen-key
# 2. List keys — note the long id
gpg --list-secret-keys --keyid-format LONG
# 3. Distribute the public key so Maven Central can verify
gpg --keyserver keyserver.ubuntu.com --send-keys <LONG_KEY_ID>
```

### 3. Maven settings.xml

Add to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>${env.SONATYPE_USERNAME}</username>
      <password>${env.SONATYPE_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

Export the secrets:

```bash
export SONATYPE_USERNAME=your-jira-user
export SONATYPE_PASSWORD='your-sonatype-token'
export GPG_PASSPHRASE='your-gpg-passphrase'
```

## Per-release flow

```bash
# 1. Make sure all tests pass (see below for the Maven dance)
mvn clean verify

# 2. Deploy to Sonatype staging
mvn clean deploy -Psonatype-oss-release

# 3. The nexus-staging-maven-plugin will:
#    - Create a staging repo
#    - Upload all signed artifacts
#    - Close the staging repo (triggers validation)
#    - Auto-release on success (because autoReleaseAfterClose=true)
#
# 4. Wait ~10 minutes, then verify on Maven Central:
#    https://central.sonatype.com/artifact/com.ai.plug/server2mcp-starter-webmvc
```

## Pre-flight checklist (before each release)

- [ ] All commits pushed to master
- [ ] CHANGELOG.md updated (TODO: add one — see "What's missing")
- [ ] Version bumped (currently `1.1.4-SNAPSHOT` → e.g. `1.1.4`)
- [ ] `git tag v1.1.4` (and `git push --tags`)
- [ ] `mvn clean verify` passes locally (tests + GPG)
- [ ] Demo e2e (21/21) verified with the staging artifacts
- [ ] README and `docs/` reflect the new version

## What's still missing (intentional)

- **CHANGELOG.md** — repo uses `docs/logs/` as its audit trail,
  but Sonatype Central doesn't read that. A top-level `CHANGELOG.md`
  summarising per-release changes is on the to-do list. Until then,
  release notes are the diff between two git tags:
  `git log --oneline v1.1.3..v1.1.4`.
- **Source attachment** is wired (maven-source-plugin). Javadoc
  generation is also wired but **may fail** on first run because
  some classes don't have Javadoc on every public method — the
  `doclint=none` config tolerates that, but the Sonatype validator
  may still reject. If it does, run with `-X` and fix the cited
  classes, or temporarily set `<failOnError>false</failOnError>` in
  the maven-javadoc-plugin config.

## Gotchas

| Symptom | Cause | Fix |
|---------|-------|-----|
| `gpg: signing failed: Inappropriate ioctl for device` | GPG agent not running in this shell | `gpgconf --launch gpg-agent` or pass `--pinentry-mode loopback` (already in pom) |
| `401 Unauthorized` from ossrh | Username/token mismatch | Re-issue token at https://s01.oss.sonatype.org → Profile → User Token |
| `Missing POM`, `Invalid packaging` | Sub-module doesn't redeclare `<parent>` | Check each module's pom references `../pom.xml` (already correct) |
| `Javadoc jar missing` | `maven-javadoc-plugin` skipped on test failure | Fix tests first, re-run |
| Stale artifacts in Central | Released a SNAPSHOT to release repo | Verify `<version>` doesn't end in `-SNAPSHOT` before `mvn deploy` |

## Verification after release

```bash
# Pull the freshly-released artifact from Central:
mvn dependency:get -Dartifact=com.ai.plug:server2mcp-starter-webmvc:1.1.4
# Or just import it in a fresh test app and run.
```

## Rollback

Sonatype Central doesn't allow deletion of released artifacts
(only deprecation). If you ship a broken release:

1. Fix forward — bump to `1.1.5`, fix the bug, re-release.
2. Mark the bad version as deprecated in Central UI.

This is by design (immutability for reproducibility).
