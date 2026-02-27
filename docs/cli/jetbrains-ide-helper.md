# JetBrains IDE Helper (`.microsmith/ide`)

Use the helper project when your repository is not Gradle-based (for example Go, .NET, Node) and
you want stronger `.microsmith.kts` type resolution in JetBrains IDEs.

## Generate or refresh helper

From repository root:

```bash
microsmith ide refresh
```

Optional flags:

- `--repo-root <path>`: generate helper for a different repository root.
- `--diagnostics <text|json>`: choose output format.
- `--verbose`: include additional diagnostic details.

## Validate helper health

```bash
microsmith ide doctor --diagnostics json --verbose
```

`ide doctor` validates helper directory/file presence and classpath synchronization, then reports
actionable remediation when helper metadata is stale.

## Generated files

Command output is constrained to `.microsmith/ide`:

- `.microsmith/ide/settings.gradle.kts`
- `.microsmith/ide/build.gradle.kts`
- `.microsmith/ide/README.md`

The generated Gradle model uses the active Microsmith runtime classpath and is idempotent.
Running `microsmith ide refresh` repeatedly only rewrites files when content changes.
The helper build uses local file dependencies only (no remote plugin repository configuration).

## JetBrains setup flow

1. Run `microsmith ide refresh`.
2. In your JetBrains IDE, link/import `.microsmith/ide/build.gradle.kts` as a Gradle project.
3. Trigger a Gradle refresh.
4. Re-run `microsmith ide refresh` after upgrading Microsmith CLI.

## Runtime contract

- Runtime generation is independent from helper usage.
- Existing `microsmith run ...` behavior remains unchanged whether `.microsmith/ide` exists or not.
