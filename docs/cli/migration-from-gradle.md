# Migration Guide: Gradle -> Microsmith CLI

This guide helps teams move generation workflows from Gradle tasks to the standalone CLI.

## What stays the same

- DSL authoring model (`microsmith { ... }`, `schemas { ... }`, `protobuf { ... }`)
- plugin architecture (`ServiceLoader`, emitter/provider model)
- generated output semantics and safe output boundary enforcement

## What changes

- No project-local Gradle wrapper required in consuming repositories
- Generation entrypoint becomes `microsmith run <script.microsmith.kts> --out <dir>`
- Common built-in plugins are bundled/pinned in official distributions (`bundled-plugins.lock`)
- plugin dependency resolution is explicit (`--plugin`, `--plugin-jar`)

## Command mapping

| Previous pattern | CLI replacement |
| --- | --- |
| Gradle task invoking DSL in JVM | `microsmith run schema.microsmith.kts --out ./generated` |
| Gradle-managed plugin dependency | `--plugin group:artifact:version` |
| local classpath jar wiring | `--plugin-jar ./path/to/plugin.jar` |
| Gradle offline mode | `--offline` |

## Step-by-step migration

1. Move DSL into a script file named `*.microsmith.kts`.
2. Replace Gradle invocation in CI with a CLI command.
3. Pin plugin coordinates and cache/lock behavior for reproducibility.
4. Enable security controls:
   - repository allowlist policy
   - checksum allowlist or lockfile validation
   - process isolation mode for stricter boundaries
5. Remove obsolete Gradle generation tasks from consumer repos.

## Recommended rollout

1. Start with one service/repository as a pilot.
2. Keep old Gradle pipeline in read-only verification mode for one release.
3. Compare generated outputs; then remove old path after parity is proven.

## Example migration snippet

Before:

```bash
./gradlew generateSchemas
```

After:

```bash
microsmith run schema.microsmith.kts --out ./generated --offline
```
