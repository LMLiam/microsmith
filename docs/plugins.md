# Plugin resolution

## Plugin resolution and security model

### Plugin inputs

Use remote plugin coordinates:

```bash
microsmith run schema.microsmith.kts --plugin com.acme:microsmith-emitter-ts:1.4.2
```

Use local plugin jars:

```bash
microsmith run schema.microsmith.kts --plugin-jar ./plugins/emitter.jar
```

Run offline after cache warmup and lock generation:

```bash
microsmith run schema.microsmith.kts --offline
```

### Security defaults

Microsmith enforces the following defaults:

- script-time dependency directives such as `@file:DependsOn` and `@file:Repository` are blocked by default
- repository access is constrained by an allowlist policy
- the built-in allowed remote repository is Maven Central: `https://repo1.maven.org/maven2`
- additional allowed repositories can be configured with `MICROSMITH_REPOSITORY_ALLOWLIST`
- `file://` repositories are blocked by default and can be explicitly enabled with `MICROSMITH_ALLOW_FILE_REPOSITORIES=true`
- generated output writes are constrained to the configured output root and reject traversal or symlink escapes
- the default execution mode is isolated per run; `--isolation process` moves execution into a separate JVM
- official CLI distributions include a pinned bundled plugin profile in `bundled-plugins.lock`

### Credentials and repository authentication

Credential precedence is deterministic:

1. `MICROSMITH_REPOSITORY_CREDENTIALS_FILE` with entries in the form `<repository-uri>|<username>|<password>`
2. GitHub Packages credentials for `https://maven.pkg.github.com` via `MICROSMITH_GITHUB_PACKAGES_USER` and `MICROSMITH_GITHUB_PACKAGES_TOKEN`, with fallback to `GITHUB_ACTOR` and `GITHUB_TOKEN`
3. global repository credentials via `MICROSMITH_REPOSITORY_USERNAME` and `MICROSMITH_REPOSITORY_PASSWORD`

Example using GitHub Packages:

```bash
export MICROSMITH_REPOSITORY_ALLOWLIST="https://maven.pkg.github.com/acme/microsmith"
export MICROSMITH_REPOSITORY_USERNAME="octocat"
export MICROSMITH_REPOSITORY_PASSWORD="$GITHUB_TOKEN"
microsmith run schema.microsmith.kts \
  --repository https://maven.pkg.github.com/acme/microsmith \
  --plugin com.acme:private-emitter:1.2.3
```

Example credentials file:

```text
# ~/.microsmith/repository-credentials.txt
https://maven.pkg.github.com/acme/microsmith|octocat|ghp_xxx
https://packages.acme.internal/maven|svc-microsmith|token-123
```

Use that file:

```bash
export MICROSMITH_REPOSITORY_CREDENTIALS_FILE="$HOME/.microsmith/repository-credentials.txt"
microsmith run schema.microsmith.kts --plugin com.acme:private-emitter:1.2.3
```

### Environment variables

| Variable                                 | Purpose                                                                                                               |
|------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| `MICROSMITH_REPOSITORY_ALLOWLIST`        | Comma-separated additional allowed repository base URIs.                                                              |
| `MICROSMITH_ALLOW_FILE_REPOSITORIES`     | Set to `true` to allow `file://` repositories for plugin coordinates.                                                 |
| `MICROSMITH_REPOSITORY_CREDENTIALS_FILE` | Path to a repository credentials file.                                                                                |
| `MICROSMITH_REPOSITORY_USERNAME`         | Default username for authenticated repository access.                                                                 |
| `MICROSMITH_REPOSITORY_PASSWORD`         | Default password or token for authenticated repository access.                                                        |
| `MICROSMITH_GITHUB_PACKAGES_USER`        | Username for GitHub Packages repository access.                                                                       |
| `MICROSMITH_GITHUB_PACKAGES_TOKEN`       | Token for GitHub Packages repository access.                                                                          |
| `MICROSMITH_PLUGIN_ALLOWLIST_FILE`       | Path to checksum allowlist file entries in the form `<code>&lt;kind&gt;&#124;&lt;key&gt;&#124;&lt;sha256&gt;</code>`. |
| `MICROSMITH_SCRIPT_CACHE_DIR`            | Override the script compilation cache directory.                                                                      |
| `MICROSMITH_PLUGIN_CACHE_DIR`            | Override the plugin resolution cache directory.                                                                       |

[Back to the README](../README.md)
