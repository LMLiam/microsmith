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

Actions:
- validate coordinate syntax: `group:artifact:version`
- confirm repository is in allowed endpoints
- update or regenerate lock/checksum metadata when plugin versions change

## Offline mode failures

Symptom:
- `--offline` run cannot resolve plugins

Actions:
- run once online to prime cache and lock metadata
- ensure plugin artifacts exist in cache directory
- rerun with `--offline` after cache warmup

## Built-in provider discovery failures

Symptom:
- CLI reports missing built-in generators/emitters

Actions:
- use official CLI distribution/fat jar
- run build integrity task in this repository: `./gradlew :cli:verifyShadowJarServices`
- avoid manually repackaging runtime jars without merged service descriptors
