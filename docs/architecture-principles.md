# Architecture principles

These rules explain why the pipeline in [Architecture](architecture.md) is shaped the way it is. Each one points at code that follows it. Copy the example, not the prose.

## Layer responsibilities

| Layer | Owns | Must not |
|---|---|---|
| `dsl-*` | Authoring surface: builders, scopes, immutable extension models such as `SchemasExtension`. | Resolve names, validate cross-object rules, render, or write files. |
| `resolve-*` | Normalisation, defaults, inheritance, and every semantic check. Produces `ResolvedModel`s. | Render, route output, or read the file system. |
| `artifact-*` | Logical artifacts and how contributions to the same artifact merge. | Validate the model. A resolved model is already valid. |
| `compile-*` | Artifact-to-artifact translation until only `TextFileArtifact` or `BinaryFileArtifact` remain. | Validate the model or look back at authoring state. |
| `gen` | Rendering, output routing, file writing, and composition of the other layers. | Own domain rules. |

Two consequences:

- `artifact-*` and `compile-*` do no semantic validation. Their `require` calls guard their own contracts, such as `ProtoFileContribution` rejecting an empty declaration list. They never re-check a rule that resolution owns.
- A resolved model carries no unresolved or nullable semantic state. A reference field in `ResolvedProtobufField.Reference` holds a `ResolvedProtobufReference` (the target identity and its declaration kind), not a name to look up later. If a downstream layer needs a null check to cope with "not resolved yet", the resolved model is incomplete.

`./gradlew verifyModuleLayers` stops a module from depending on a later layer. See [Module layering](contributing.md#module-layering).

## Parse, don't validate

Resolution turns loose authoring data into types that cannot hold invalid values. Later layers then take those types on trust.

- Resolved models are sealed: a private constructor and a companion `of` that returns `EitherNel<Rejection, Model>`. `ResolvedProtobufSchemaModel.of` and `ResolvedProtobufMessage.of` are the reference. `@ConsistentCopyVisibility` keeps `copy` from bypassing the constructor.
- Small value types follow the same shape with `parse`: `ProtoIdentifier.parse`, `FieldNumber.parse`.
- `of` collects every rejection it finds, not the first one.
- The resolver maps each rejection to a `ResolutionIssue` at its boundary. See `ProtobufRpcSchemasResolver`, which calls `ResolvedProtobufRpcSchemaModel.of` and maps the left side with `toIssue()`.
- Downstream code, such as `ProtobufArtifactContributor`, reads the model and does not check it again.

## Typed expected failures

An authoring mistake is an expected failure. It is data, not an exception.

- Each domain defines a sealed `ResolutionIssue` hierarchy. See `ProtobufResolutionIssue` and `ProtobufRpcResolutionIssue`.
- Resolvers accumulate issues with Arrow: `EitherNel`, `zipOrAccumulate`, and `mapOrAccumulate`. `ProtobufDeclarationResolver.resolveMessage` runs five independent checks and reports all that fail.
- `DomainResolver.resolve` returns `DomainResolution`: `NotApplicable` when the extension holds nothing for this resolver, `Success` with the model, or `Failure` with a non-empty list of issues. `DomainResolutionService` merges the outcomes of all resolvers into one `ResolutionOutcome`.
- A `ResolutionIssueDiagnosticMapper<I>` turns an issue into a `ResolutionDiagnostic(code, message)`. The code is stable and namespaced by domain: `protobuf.*`, `protobuf.rpc.*`, `dotnet.asp.*`. Mappers are discovered by `ServiceLoader`. An issue with no mapper gets the fallback code `resolution.unmapped-issue`.
- Exceptions are for programming errors and infrastructure failures: a duplicate resolver registration, a mapper whose `issueType` does not match its generic type, an unreadable file. They are not for a user's schema.

## How a resolution failure reaches the user

One coded diagnostic per issue, end to end.

1. `MicrosmithGenerationRunner` receives `ResolutionOutcome.Failure`, asks `ResolutionDiagnosticService` to describe each issue, and throws `GenerationResolutionFailedException`.
2. The script host catches it and returns a `ScriptRunFailure` of type `ScriptFailureType.RESOLUTION`. It holds one `ScriptRunDiagnostic(message, code)` per issue.
3. The CLI reports each as its own error event under `MS-CLI-2005`, with the issue code in `issueCode` for JSON output.

The exit codes and the JSON contract are in [Command line interface](cli.md#diagnostics-contract). Do not repeat them elsewhere.

## Context parameters

Kotlin context parameters are allowed where an operation has an environment that every helper in the call tree needs and that is not part of any helper's own question.

Good fit: `ProtobufReferenceResolver.resolve` declares `context(identity: QualifiedSchemaName)`. The schema being resolved is the scope of the whole operation. Every helper needs it to report an issue against the right schema, and passing it through each call would only add noise. `ProtobufDeclarationResolver` also uses `context(..., _: Raise<ProtobufResolutionIssue>)` and `RaiseAccumulate` for the Arrow error channel.

Poor fit:

- A long-lived collaborator. Use constructor injection, as `ProtobufDeclarationResolver` does for `ProtobufReferenceResolver`.
- A value that is a real input of the function. Make it a parameter so callers and tests see it.
- A public plugin API. Context parameters would leak into every plugin signature.
- Anything that makes a function hard to call from a test without a `with(...)` block.

## Metro is internal

Metro wires the generation pipeline together. It stays out of every published API:

- `AppGraph` is an `internal` graph that lives for the JVM. It holds the injected seams (file system, clock, environment, console, diagnostic sink, dispatcher) and nothing that depends on the plugin class loader. `createMicrosmithGenerator` builds it from the seam implementations that the caller passes in.
- `RunGraph` is an `internal` graph extension that `MicrosmithGenerator.generate` creates for each call. It takes the plugin class loader explicitly, discovers the plugins with it, binds the built-in text and binary renderers and assemblers, and builds the pipeline services. Two concurrent calls share no run state.
- `gen` has no `object` that holds behaviour. Progress and warnings go to the injected `DiagnosticSink`. `gen` reads the process environment and system properties only in `SystemEnvironment`, and writes to the standard streams only in `SystemConsole`. detekt bans the other calls that reach them.
- No public plugin API mentions Metro. Plugin contracts are plain interfaces such as `DomainResolver` and `ArtifactCompiler`.
- Any module may use Metro internally, but a published API module must not mention a Metro type in its ABI dump. `verifyModuleLayers` enforces this.

## ServiceLoader boundary

Plugins join the pipeline through `java.util.ServiceLoader`, nothing else.

- `RunGraph` discovers `MicrosmithPlugin` implementations with the plugin class loader, sorts them by `id`, rejects duplicate ids, and calls `services(host)` on each. Every plugin gets its own `HostServices`, whose `DiagnosticSink` tags each diagnostic with the plugin id.
- The same run also loads the old contracts: `DomainResolver`, `ArtifactContributor`, `ArtifactAssembler`, `ArtifactCompiler`, `ArtifactRenderer`, and `ResolutionIssueDiagnosticMapper`. `discoverMicrosmithPlugins(classLoader)` fills a `MicrosmithPluginCatalog` with them. Both paths coexist until the old contracts are removed.
- Two compilers, assemblers or renderers for one artifact type are a `PluginConflictException` that names the plugins, the host or the old contracts that supplied them. Two outputs with the same path are also a `PluginConflictException`. How contributions to one artifact combine is the artifact's `ArtifactAssembler.merge`, which the host does not second-guess.
- An exception while discovering a provider or in `services(host)` fails the run before the pipeline starts, with a message that names the plugin id, or the provider class and jar. A `LinkageError` is reported as an incompatible plugin. There is no partial loading.
- The host closes every `PluginServices` in reverse order in a non-cancellable `finally`. A failing `close` is a warning, and a suppressed exception of the original failure when the run failed.
- `ScriptSymbolContributorRegistry` does the same for `ScriptSymbolContributor`.
- `discoverMicrosmithPlugins` reads the class loader that the run graph gives it. `ScriptSymbolContributorRegistry` reads the thread context class loader and falls back to its own.
- `TextFileArtifact` and `BinaryFileArtifact` are not discovered. The run graph binds their assemblers and renderers.
- The CLI resolves `--plugin` and `--plugin-jar` into a plugin classpath. The script host wraps it in a `URLClassLoader` and sets it as the context class loader for the run, and passes it to `generate`. See [Plugin resolution](plugins.md).
- The Gradle plugin passes the `microsmithPlugins` configuration as that classpath to its worker JVM. See [Gradle plugin](gradle.md).
- The Maven plugin loads the `<dependencies>` of its `<plugin>` entry. See [Maven plugin](maven.md).

Plugin authors write against this boundary. See [Plugin development](plugin-development.md).

## Mappie for structural copies

Mappie copies fields between types that have the same shape, so a reviewer does not read forty lines of `Foo(a = x.a, b = x.b)`.

- Use it for structural copies, such as resolved model to artifact. Examples: `ProtobufRpcOperationMapper` in `artifact-schemas-protobuf-rpc` and `DotnetPackageVersionMapper` in `artifact-services-dotnet-packages`.
- One internal `ObjectMappie` object per file, in the package of the types it maps.
- Keep allocation, naming, and ordering by hand. A port number, a generated name, or a sort order is a decision, not a copy.
- Mappie copies fields. It does not turn a sealed hierarchy into a plain value. Write a small exhaustive `when` for that. `DotnetAspRequestFieldArtifactMapper` maps the structure with Mappie and unwraps the sealed `DotnetAspDefaultValue` with a `when` beside it.

[Back to the README](../README.md)
