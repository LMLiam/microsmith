# Plugin development

This guide is for authors of third-party Microsmith plugins. To use a plugin, see [Plugin resolution](plugins.md). For the reasons behind the pipeline, see [Architecture principles](architecture-principles.md).

A plugin is a jar. It adds some of the stages below, and each stage is an interface that Microsmith finds with `java.util.ServiceLoader`.

| Step | You write | Contract | Module |
|---|---|---|---|
| 1 | Authoring DSL | `MicrosmithExtension`, `MicrosmithScope` extension functions | `dsl` |
| 2 | Script symbols (optional) | `ScriptSymbolContributor` | `runtime-scripting-api` |
| 3 | Resolution | `DomainResolver`, `ResolvedModel`, `ResolutionIssue`, `ResolutionIssueDiagnosticMapper` | `resolve` |
| 4 | Artifacts | `ArtifactContributor`, `ArtifactAssembler`, `Artifact`, `ArtifactId`, `ArtifactContribution` | `artifact` |
| 5 | Compilation | `ArtifactCompiler` | `compile` |
| 6 | Registration | `META-INF/services` entries | none |

Every module is published to GitHub Packages as `io.github.lmliam.microsmith:<module>`.

Snippets omit imports. The examples below build one small family, `changelog { entry("...") }`, that writes `CHANGELOG.md`. Every contract they implement exists in the repository. The protobuf modules show the same shapes at full size.

## 1. Authoring DSL

A root extension is an immutable class that implements `MicrosmithExtension`. Implement `MergeableExtension` as well when a second `changelog { }` block should add to the first. Without it, the later block replaces the earlier one.

```kotlin
data class ChangelogExtension(val entries: List<String>) :
    MicrosmithExtension, MergeableExtension<ChangelogExtension> {
    override fun merge(other: ChangelogExtension) = ChangelogExtension(entries + other.entries)
}

@MicrosmithDsl
class ChangelogScope {
    internal val entries = mutableListOf<String>()

    fun entry(text: String) {
        entries += text
    }
}

fun MicrosmithScope.changelog(block: ChangelogScope.() -> Unit) {
    val builder = this as? MicrosmithBuilder ?: error("changelog { ... } can only be invoked within microsmith { ... }")
    builder.put(ChangelogExtension(ChangelogScope().apply(block).entries.toList()))
}
```

`SchemasExtension` and `MicrosmithScope.schemas` follow the same shape. Mark every scope with `@MicrosmithDsl`.

The authoring layer only collects. Do not check names, references, or cross-object rules here. That is step 3.

Scripts get a fixed set of default imports, so a script imports your entry point with an ordinary `import`:

```kotlin
import com.acme.changelog.changelog

microsmith {
    changelog { entry("Fix login redirect") }
}
```

### Joining an existing family

To add a schema dialect, implement `Schema` and `SchemaType` (see `ProtobufSchema` and `ProtobufSchemaType`). Add a `SchemasScope` extension function that casts to `SchemasBuilder` and calls `register` (see `SchemasScope.protobuf`). Your dialect then needs no root extension of its own.

## 2. Script symbols (optional)

Skip this unless scripts should get a typed value for a name they declare. The built-in example: after `message("GetUserRequest")`, a script can write `request(GetUserRequest)`. The value is a `MessageRef`. It is produced by `ProtobufScriptSymbolContributor` in `runtime-scripting`.

```kotlin
class AcmeScriptSymbolContributor : ScriptSymbolContributor {
    override val id = "acme.topics"
    override val declarationCallNames = setOf("topic")

    override fun discover(site: ScriptSymbolDeclarationSite): ScriptSymbolDefinition? {
        val name = (site.call.arguments.firstOrNull() as? ScriptLiteral.StringValue)?.value ?: return null
        return ScriptSymbolDefinition(propertyName = name, valueType = TopicRef::class, kind = "topic", valueKey = name)
    }

    override fun createValue(kind: String, valueKey: String): Any = TopicRef(valueKey)
}
```

- `id` must be unique across contributors. A duplicate fails the run.
- A call whose name is not a string literal, or not a Kotlin identifier, produces no symbol.
- Two declarations with the same property name and different values fail script compilation with an ambiguity error, even if no reference uses the name.
- `valueType` should be a type your DSL overloads accept, such as `MessageRef`.

Depend on `runtime-scripting-api` only.

## 3. Resolution

Resolution turns your extension into a resolved model. Do all semantic checking here and report failures as typed issues.

```kotlin
sealed interface ChangelogIssue : ResolutionIssue {
    data class BlankEntry(val position: Int) : ChangelogIssue
}

@JvmInline
value class ChangelogEntry private constructor(val text: String) {
    companion object {
        fun parse(position: Int, raw: String): Either<ChangelogIssue, ChangelogEntry> =
            if (raw.isBlank()) Either.Left(ChangelogIssue.BlankEntry(position)) else Either.Right(ChangelogEntry(raw))
    }
}

@ConsistentCopyVisibility
data class ResolvedChangelog private constructor(val entries: List<ChangelogEntry>) : ResolvedModel {
    companion object {
        fun of(raw: List<String>): EitherNel<ChangelogIssue, ResolvedChangelog> =
            raw.withIndex().mapOrAccumulate { (index, text) -> ChangelogEntry.parse(index, text).bind() }
                .map(::ResolvedChangelog)
    }
}

class ChangelogResolver : DomainResolver<ChangelogExtension, ResolvedChangelog> {
    override val authoringType = ChangelogExtension::class
    override val resolvedType = ResolvedChangelog::class

    override fun resolve(authoring: ChangelogExtension): DomainResolution<ResolvedChangelog> {
        if (authoring.entries.isEmpty()) return DomainResolution.NotApplicable
        return ResolvedChangelog.of(authoring.entries).fold(
            ifLeft = { DomainResolution.Failure(it) },
            ifRight = { DomainResolution.Success(it) },
        )
    }
}
```

- `authoringType` and `resolvedType` must match the generic arguments and be exact classes. Matching is by exact class, not by subtype.
- A resolver returns `NotApplicable` when the extension holds nothing for it. Several resolvers can share one authoring type, as the protobuf and protobuf RPC resolvers share `SchemasExtension`. Register one implementation class per resolver.
- `mapOrAccumulate` here is `arrow.core.mapOrAccumulate`. The `arrow.core.raise` overload has a different receiver.
- Build the resolved model through `of`, with a private constructor, so an invalid model cannot exist. See `ResolvedProtobufSchemaModel`.
- A schema dialect makes its model implement `ResolvedSchemasModel`. A service family implements `ResolvedServicesModel`. Both are marker interfaces that add no members.

Each issue type needs a mapper. The code is stable and namespaced by your plugin.

```kotlin
class ChangelogDiagnosticMapper : ResolutionIssueDiagnosticMapper<ChangelogIssue> {
    override val issueType = ChangelogIssue::class

    override fun map(issue: ChangelogIssue): ResolutionDiagnostic =
        when (issue) {
            is ChangelogIssue.BlankEntry ->
                ResolutionDiagnostic("changelog.blank-entry", "Changelog entry ${issue.position} is blank.")
        }
}
```

An issue with no mapper still fails the run, but with the generic code `resolution.unmapped-issue`. `ProtobufRpcResolutionDiagnosticMapper` is a full example.

Do not throw for a user's mistake. Return issues. See [How a resolution failure reaches the user](architecture-principles.md#how-a-resolution-failure-reaches-the-user).

## 4. Artifacts

An artifact is a logical output that exists before rendering. A contributor adds contributions, and an assembler merges contributions to the same artifact.

```kotlin
data object ChangelogArtifactId : ArtifactId<ChangelogArtifact> {
    override val artifactType = ChangelogArtifact::class
}

data class ChangelogArtifact(val entries: List<String>) : Artifact {
    override val id = ChangelogArtifactId
}

data class ChangelogContribution(val entries: List<String>) : ArtifactContribution<ChangelogArtifact> {
    override val artifactId = ChangelogArtifactId
}

class ChangelogArtifactContributor : ArtifactContributor<ResolvedChangelog> {
    override val resolvedType = ResolvedChangelog::class

    override fun contribute(model: ResolvedChangelog): List<ArtifactContribution<Artifact>> =
        listOf(ChangelogContribution(model.entries.map(ChangelogEntry::text)))
}

class ChangelogArtifactAssembler : ArtifactAssembler<ChangelogArtifact> {
    override val artifactType = ChangelogArtifact::class

    override fun create(contribution: ArtifactContribution<ChangelogArtifact>) =
        ChangelogArtifact(contribution.entries())

    override fun merge(current: ChangelogArtifact, contribution: ArtifactContribution<ChangelogArtifact>) =
        current.copy(entries = current.entries + contribution.entries())
}

private fun ArtifactContribution<ChangelogArtifact>.entries(): List<String> {
    require(this is ChangelogContribution) { "Unsupported changelog contribution: ${this::class}" }
    return entries
}
```

- The contributor trusts the resolved model. It does no validation.
- `merge` decides what two contributions to one artifact mean: combine them, or reject a conflict. `TextFileArtifactAssembler` rejects different contents for the same path.
- A schema dialect makes its artifacts implement `SchemasArtifact`. A service family uses `ServicesArtifact`. `ProtobufArtifact` and `ProtoFileArtifact` show the layering.

A contributor can also emit `TextFileArtifactContribution` directly, which skips your own artifact, assembler, and compiler. Do this when the output is one file and nothing else needs to merge into it.

## 5. Compilation

A compiler turns one artifact into contributions to other artifacts. Compilation repeats until only file artifacts remain. A compiler must not produce a contribution of its own artifact type.

```kotlin
class ChangelogArtifactCompiler : ArtifactCompiler<ChangelogArtifact> {
    override val artifactType = ChangelogArtifact::class

    override fun compile(artifact: ChangelogArtifact): List<ArtifactContribution<Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(relativePath = Path.of("CHANGELOG.md")),
                contents = artifact.entries.joinToString("\n") { "- $it" },
            )
        )
}
```

`TextFileArtifact` and `BinaryFileArtifact` have built-in assemblers and renderers, so a plugin does not write an `ArtifactRenderer`. Write one only for a new kind of final file artifact.

A schema dialect implements `SchemasArtifactCompiler`. A service family implements `ServicesArtifactCompiler`. See `ProtoFileArtifactCompiler`.

## 6. Registration

Microsmith finds a plugin through `ServiceLoader`. The jar must contain one file per contract, named after the contract's fully qualified name, with one implementation class per line.

| File under `META-INF/services/` | Lists |
|---|---|
| `io.github.lmliam.microsmith.resolve.DomainResolver` | resolvers |
| `io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper` | issue mappers |
| `io.github.lmliam.microsmith.artifact.ArtifactContributor` | contributors |
| `io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler` | assemblers |
| `io.github.lmliam.microsmith.compile.ArtifactCompiler` | compilers |
| `io.github.lmliam.microsmith.gen.ArtifactRenderer` | renderers |
| `io.github.lmliam.microsmith.runtime.scripting.api.ScriptSymbolContributor` | script symbol contributors |

Each implementation needs a public no-argument constructor.

This repository generates those files with `@ServiceProvider` from `com.github.eventhorizonlab.spi`. Its modules add `io.github.eventhorizonlab:spi-tooling-annotations` as a dependency and `io.github.eventhorizonlab:spi-tooling-processor` to `kapt`. The version is `spiTooling` in `gradle/libs.versions.toml`.

```kotlin
@ServiceProvider(DomainResolver::class)
class ChangelogResolver : DomainResolver<ChangelogExtension, ResolvedChangelog> { /* ... */ }
```

You can use that processor or write the files by hand. `ScriptSymbolContributor` is not marked as a service contract, so register it by hand or with another ServiceLoader generator.

Ship the DSL types in the same jar. Users add the jar with `--plugin` or `--plugin-jar`, or through the Gradle and Maven plugins. See [Plugin resolution](plugins.md), [Gradle plugin](gradle.md), and [Maven plugin](maven.md).

## 7. Testing

Microsmith ships no test-helper module. Test through the public API.

Resolver and mapper, with no process or files:

```kotlin
class ChangelogResolverTests :
    StringSpec({
        "reports each blank entry as its own coded diagnostic" {
            val model = microsmith { changelog { entry(" "); entry("Fix login"); entry("") } }

            val outcome = DomainResolutionService(listOf(ChangelogResolver())).resolve(model)

            val failure = outcome.shouldBeTypeOf<ResolutionOutcome.Failure>()
            ResolutionDiagnosticService(listOf(ChangelogDiagnosticMapper()))
                .describe(failure.issues)
                .map { it.code } shouldContainExactly listOf("changelog.blank-entry", "changelog.blank-entry")
        }
    })
```

Whole pipeline, with real discovery. Put the plugin and the `gen` module on the test classpath, then call the suspend function `MicrosmithModel.generateTo(outputDir)` on a temporary directory and read the files. `ServiceLoader` finds your registrations on the context class loader.

Script to file, as a user runs it. `MicrosmithScriptHost().run(ScriptRunRequest(script, outputDir, variables, flags, pluginClasspath = listOf(pluginJar)))` returns a `ScriptRunSuccess`, or a `ScriptRunFailure` whose `type` is `ScriptFailureType.RESOLUTION` and whose `diagnostics` carry your codes. `MicrosmithScriptHostTests` in `runtime-scripting` does this for the built-in plugins.

## What a plugin does not need to know

- Metro and `MicrosmithGenerationGraph`. Composition is internal to `gen`, and no plugin contract mentions it.
- Isolation modes. The host runs a script in a class loader or a separate JVM. Your plugin is loaded the same way in both.
- CLI internals: argument parsing, diagnostics formatting, exit codes, and the plugin cache.
- Gradle and Maven wiring. They only put your jar on the plugin classpath.
- Output routing and file writing. Return file artifacts and `gen` places them.

[Back to the README](../README.md)
