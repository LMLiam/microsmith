# Troubleshooting

## Java runtime issues

Symptoms:

- `java` not found
- unsupported class version or runtime mismatch

What to do:

- use the canonical installer path so Java 24 is provisioned automatically when needed
- if you are using a manual channel, install Java 24+ and set `JAVA_HOME`
- rerun `microsmith --version` and `microsmith --help`

## Installer failures

Symptoms:

- installer exits non-zero
- checksum mismatch during installation
- runtime provisioning fails

What to do:

- verify the relevant `*.sha256` file and rerun the installer
- use explicit archive and checksum flags when diagnosing a specific asset
- inspect installer output for missing prerequisites such as `curl`, `tar`, `unzip`, or `python3`
- do not combine `--force-runtime-provision` and `--skip-runtime-provision`

## Script and init failures

Symptoms:

- `microsmith run` exits with compile or evaluation errors
- `microsmith init` exits non-zero
- expected bootstrap files are missing

What to do:

- confirm the script file ends with `.microsmith.kts`
- rerun with `--diagnostics json --verbose`
- ensure `--repo-root` points to an existing directory
- run `microsmith doctor --diagnostics json --verbose` to detect incomplete bootstrap state
- resolve filesystem conflicts such as a directory already existing at `build.microsmith.kts`
- rerun `microsmith init --force` if you want the managed bootstrap scripts to replace existing regular files
- if helper generation was skipped earlier, run `microsmith ide refresh` before importing `.microsmith/ide` into JetBrains IDEs

## Resolver, credentials, and offline failures

Symptoms:

- plugin coordinate cannot be resolved
- repository URI is rejected
- authentication fails
- checksum or lockfile validation fails
- `--offline` cannot resolve plugins

What to do:

- validate coordinate syntax: `group:artifact:version`
- confirm the repository is in the allowlist
- configure credentials using the precedence in [Plugin resolution](plugins.md)
- run once without `--offline` to warm the cache and write lock metadata
- ensure the complete locked dependency graph exists in the plugin cache
- if strict checksum allowlisting is enabled, include transitive entries such as `remote-artifact|<cache-relative-path>|<sha256>`

## Built-in provider or IDE helper failures

Symptoms:

- built-in resolvers, artifact contributors, assemblers, compilers, or renderers are reported missing
- `.microsmith.kts` files still show unresolved Microsmith symbols in JetBrains IDEs
- helper project appears stale after a CLI upgrade

What to do:

- use the official CLI distribution or installer
- run `microsmith doctor --diagnostics json --verbose`
- run `microsmith ide doctor --diagnostics json --verbose`
- rerun `microsmith ide refresh`
- in JetBrains IDEs, re-import or refresh `.microsmith/ide/build.gradle.kts`
- if you are validating release assets in this repository, run `./gradlew :cli:verifyShadowJarServices` and `./gradlew :cli:verifyDistLayout`

[Back to the README](../README.md)
