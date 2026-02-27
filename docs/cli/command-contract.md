# CLI Command Contract (`init` + `ide`)

This document is the source of truth for setup and IDE workflow command behavior.

## Canonical command surface

- `microsmith init [--repo-root <path>] [--non-interactive] [--yes] [--diagnostics <text|json>] [--verbose]`
- `microsmith ide refresh [--repo-root <path>] [--diagnostics <text|json>] [--verbose]`
- `microsmith ide doctor [--repo-root <path>] [--diagnostics <text|json>] [--verbose]`
- `microsmith --version`

## Default `init` behavior

`microsmith init` is deterministic and non-destructive by default:

- creates `settings.microsmith.kts` when missing
- creates `build.microsmith.kts` when missing
- preserves existing bootstrap files (does not overwrite)
- refreshes `.microsmith/ide/*` helper metadata
- prints exact immediate next generation command

After successful init, no mandatory IDE follow-up command is required for the default flow.

## Canonical happy paths

Local:

```bash
microsmith init
microsmith run build.microsmith.kts --out ./generated
```

CI non-interactive:

```bash
microsmith init --non-interactive --yes --diagnostics json --verbose
microsmith run build.microsmith.kts --out ./generated --diagnostics json --verbose
```

Maintenance:

```bash
microsmith ide doctor --diagnostics json --verbose
microsmith ide refresh
```

## Non-interactive contract

- `--non-interactive` and `--yes` are supported for CI automation.
- No interactive prompts are emitted in non-interactive mode.
- Diagnostics output remains deterministic with `--diagnostics json`.

## Exit codes

| Category | Code | Exit | Meaning |
|---|---|---:|---|
| Usage | `MS-CLI-0001` | `2` | Invalid command/flags. |
| IDE refresh failure | `MS-CLI-4001` | `40` | `ide refresh` failed. |
| IDE doctor failure | `MS-CLI-4101` | `41` | `ide doctor` detected issues or failed. |
| Init conflict | `MS-CLI-5001` | `50` | `init` detected conflicting filesystem state. |
| Init validation failure | `MS-CLI-5002` | `51` | `init` input/environment validation failed. |
| Init runtime failure | `MS-CLI-5003` | `52` | `init` failed unexpectedly at runtime. |

## Machine-readable diagnostics contract

When `--diagnostics json` is used, each event is a single JSON line with:

- `timestamp` (UTC instant)
- `level` (`info`, `warn`, `error`)
- `message`
- `code` (for error events)
- `details` (included when `--verbose` is enabled)

This contract is stable for automation and CI parsing.
