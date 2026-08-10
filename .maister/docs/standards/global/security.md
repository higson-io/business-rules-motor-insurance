## Secrets & Credentials

This is a demo project, but the secret-handling rule is not relaxed. Demo credentials are acceptable only where the resource they unlock is disposable; anything that touches a real environment must be supplied at invocation time, never committed.

### Never Commit Credentials — Override at Invocation Time
Credentials and integration tokens must not be committed. Supply a real token with `-D` on the command line or a Maven `settings.xml` server entry.

```bash
mvn higson-cli:push -Dhigson.studio.token=$HIGSON_TOKEN
```

*Evidence:* pom.xml warning "Do NOT reuse for real environments. Prefer overriding with -Dhigson.studio.token=... or settings.xml instead of committing a secret."; push-to-docker.sh takes registry credentials as script arguments; docker-compose.yml carries only throwaway sa/sa demo credentials for the disposable H2 database.

### Resolved: The Studio Token Literal Was Removed Before `pom.xml` Was Committed
The working tree's `pom.xml` once carried a hard-coded Higson Studio integration JWT with admin authorities (`SYSTEM_CONFIG_ACCESS`, `PROD_W_ALL`, `SNAPSHOT_UPLOAD`) expiring in 2049, on a repository whose remote is public. It was **never committed**: the literal was replaced with an empty default and a comment before `pom.xml` first entered history, in commit `d09c201`.

`<higson.studio.token></higson.studio.token>` must stay empty. Supply the real value per invocation, per the rule above.

⚠️ **Re-check before every `pom.xml` commit.** The token is easy to paste back in while working with Higson CLI locally, and nothing in the repository prevents it — there is no pre-commit hook and no secret scanning. `grep -c 'eyJ' pom.xml` should return `0`.

*Evidence:* `git show HEAD:pom.xml` shows an empty token property; `grep -c 'eyJ' pom.xml` returns 0 in the working tree.
