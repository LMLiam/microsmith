# JetBrains IDE helper

Use the helper project when you want stronger `.microsmith.kts` type resolution in repositories that intentionally keep Microsmith outside the host build, or in non-Gradle repositories such as Go, .NET, Node, Python, Ruby, and Rust.

For Java and Kotlin repositories that already use Gradle, prefer the [native Gradle integration](gradle.md) first. Use the helper only when you intentionally keep Microsmith CLI-managed instead of adopting the Gradle plugin.

`microsmith init` already refreshes the helper by default. Use `microsmith ide refresh` when you skipped helper generation during init or need to repair or resynchronize the helper later.

Generate or refresh the helper:

```bash
microsmith ide refresh
```

Validate helper health:

```bash
microsmith ide doctor --diagnostics json --verbose
```

Optional flags:

- `--repo-root <path>`
- `--diagnostics <text|json>`
- `--verbose`

Generated files:

- `.microsmith/ide/settings.gradle.kts`
- `.microsmith/ide/build.gradle.kts`
- `.microsmith/ide/README.md`

JetBrains workflow:

1. Run `microsmith init` from the repository root, or `microsmith ide refresh` if helper generation was skipped earlier.
2. Link or import `.microsmith/ide/build.gradle.kts` as a Gradle project in the IDE.
3. Refresh Gradle indexing.
4. Rerun `microsmith ide refresh` after upgrading the Microsmith CLI or changing plugin dependencies.

Node-specific guidance:

- for Node repositories, WebStorm is the recommended JetBrains IDE path when you want `.microsmith.kts` authoring support
- native Node indexing does not resolve Microsmith script symbols on its own; use the helper project or the fallback jar when `.microsmith.kts` files need IDE type resolution

Go-specific guidance:

- for Go repositories, GoLand is the supported JetBrains IDE path when you want `.microsmith.kts` authoring support
- native Go indexing does not resolve Microsmith script symbols on its own; use the helper project or the fallback jar when `.microsmith.kts` files need IDE type resolution

Python-specific guidance:

- for Python repositories, PyCharm is the supported JetBrains IDE path when you want `.microsmith.kts` authoring support
- native Python indexing does not resolve Microsmith script symbols on its own; use the helper project or the fallback jar when `.microsmith.kts` files need IDE type resolution

.NET-specific guidance:

- for .NET repositories, Rider is the supported JetBrains IDE path when you want `.microsmith.kts` authoring support
- native .NET indexing does not resolve Microsmith script symbols on its own; use the helper project or the fallback jar when `.microsmith.kts` files need IDE type resolution

Java-specific guidance:

- for Java repositories, IntelliJ IDEA is the recommended JetBrains IDE path when you want `.microsmith.kts` authoring support
- Gradle-based Java repositories should prefer the native Gradle plugin path; use the helper or fallback for Maven and CLI-managed Java repositories

Kotlin-specific guidance:

- for Kotlin repositories, IntelliJ IDEA is the recommended JetBrains IDE path when you want `.microsmith.kts` authoring support
- Gradle-based Kotlin repositories should prefer the native Gradle plugin path; use the helper or fallback for Maven and CLI-managed Kotlin repositories

Ruby-specific guidance:

- for Ruby repositories, RubyMine is the recommended JetBrains IDE path when you want `.microsmith.kts` authoring support
- native Ruby indexing does not resolve Microsmith script symbols on its own; use the helper project or the fallback jar when `.microsmith.kts` files need IDE type resolution

Rust-specific guidance:

- for Rust repositories, RustRover is the recommended JetBrains IDE path when you want `.microsmith.kts` authoring support
- native Cargo indexing does not resolve Microsmith script symbols on its own; use the helper project or the fallback jar when `.microsmith.kts` files need IDE type resolution

## When to use each command

- First-time setup in a consumer repository: `microsmith init`
- Helper was skipped, removed, or needs regeneration after CLI or plugin changes: `microsmith ide refresh`
- Symbols are still unresolved or the helper appears stale: `microsmith ide doctor --diagnostics json --verbose`

## Repository hygiene

Treat `.microsmith/ide/` as local generated IDE state.

Why:
- the generated helper build uses local file dependencies that resolve against the current Microsmith runtime installation
- `microsmith ide refresh` owns the helper files and will rewrite them when the managed content changes
- runtime generation does not depend on the IDE helper being present

Recommended policy for consumer repositories:

```gitignore
.microsmith/ide/
```

Ownership and overwrite rules:

- Microsmith manages only `.microsmith/ide/settings.gradle.kts`, `.microsmith/ide/build.gradle.kts`, and `.microsmith/ide/README.md`
- do not hand-edit those managed files; rerun `microsmith ide refresh` instead
- rerun `microsmith ide refresh` after upgrading the Microsmith CLI or changing plugin dependencies
- if `microsmith ide doctor` reports missing or stale helper state, repair it with `microsmith ide refresh`

Important constraints:

- runtime generation does not depend on the IDE helper
- the helper build uses local file dependencies only
- repeated refreshes only rewrite files when content changes

## Script-definition fallback

Use the fallback only when you explicitly do not want the generated `.microsmith/ide` helper project.

Choose between the two paths like this:

- prefer the helper when you want the strongest JetBrains IDE experience and automatic synchronization with the local Microsmith runtime classpath
- use the fallback when you want a smaller repository footprint and are willing to attach a library manually

Fallback artifact sources:

- GitHub Release asset: `microscript-definition-<version>-all.jar`
- local build: `./gradlew :runtime-scripting:ideFallbackArtifacts`
- published package: `io.github.lmliam.microsmith:runtime-scripting:<version>:all`

Manual JetBrains setup:

1. Download the fallback jar that matches your Microsmith version, or build it locally.
2. In IntelliJ IDEA, GoLand, Rider, PyCharm, RubyMine, or RustRover, add the jar as a project library from the IDE project settings.
3. Reopen or reindex `.microsmith.kts` files.
4. Replace the jar after upgrading Microsmith.

Limitations versus the helper path:

- fallback setup is manual and more IDE-version-sensitive
- the fallback jar does not keep project-local plugin classpaths synchronized automatically
- plugin-provided types from `--plugin` or `--plugin-jar` are not exposed automatically through this fallback
- if symbols remain unresolved after attaching the jar, prefer the helper workflow instead

[Back to the README](../README.md)
