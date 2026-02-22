# Troubleshooting

## Java runtime errors

Symptom:
- `java` not found
- unsupported class version / runtime mismatch

Actions:
- install Java 24+
- set `JAVA_HOME` to the Java 24 installation
- rerun `microsmith --help` to verify launcher/runtime path

## Script diagnostics

Symptom:
- non-zero exit with compile/evaluation errors

Actions:
- confirm script extension is `.microsmith.kts`
- use deterministic diagnostics in CLI output (`[error] file:line:column`)
- fix unknown DSL calls and missing imports

## Resolver and plugin failures

Symptom:
- plugin coordinate cannot be resolved
- repository URI rejected
- checksum mismatch
- lockfile validation errors (`[lockfile]`)
- authentication errors (`[authentication]`)
- repository policy blocks (`[repository-policy]`)

Actions:
- validate coordinate syntax: `group:artifact:version`
- confirm repository is in allowed endpoints
- for private repositories, configure credentials using one of:
  - `MICROSMITH_REPOSITORY_CREDENTIALS_FILE`
  - `MICROSMITH_REPOSITORY_USERNAME` + `MICROSMITH_REPOSITORY_PASSWORD`
  - `MICROSMITH_GITHUB_PACKAGES_USER` + `MICROSMITH_GITHUB_PACKAGES_TOKEN`
    (fallback: `GITHUB_ACTOR` + `GITHUB_TOKEN`)
- update or regenerate lock/checksum metadata when plugin versions change
- if lockfile diagnostics are reported, regenerate lock metadata with a non-offline run
- for strict allowlist mode, include transitive graph entries (`remote-artifact|<cache-relative-path>|<sha256>`) in the allowlist file
- verify diagnostics do not contain secret material; Microsmith redacts configured tokens/passwords by default

## Offline mode failures

Symptom:
- `--offline` run cannot resolve plugins

Actions:
- ensure lockfile metadata exists and is on version 2
- run once online to prime cache and generate lock metadata when missing
- ensure the full locked dependency graph exists in plugin cache directory
- rerun with `--offline` after cache warmup

## Built-in provider discovery failures

Symptom:
- CLI reports missing built-in generators/emitters

Actions:
- use official CLI distribution/fat jar
- run build integrity task in this repository: `./gradlew :cli:verifyShadowJarServices`
- run distribution integrity task when validating release assets: `./gradlew :cli:verifyDistLayout`
- inspect `bundled-plugins.lock` in the unpacked distribution and confirm it lists expected bundled providers
- avoid manually repackaging runtime jars without merged service descriptors

## JetBrains IDE helper issues

Symptom:
- `.microsmith.kts` files still show unresolved Microsmith symbols in JetBrains IDEs
- helper project appears stale after CLI upgrade

Actions:
- run `microsmith ide refresh` from repository root
- in JetBrains IDE, re-import or refresh `.microsmith/ide/build.gradle.kts`
- verify helper files exist under `.microsmith/ide/` and were updated
- if repository root is non-standard, rerun with `--repo-root <path>`
