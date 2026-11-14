# Dependency injection in 4.0

**Status:** Accepted

**Milestone:** v4.0.0, together with #203 (explicit API mode)

## Summary

Today only `gen` uses Metro. Every other module builds its own collaborators, keeps behaviour in `object`s, and calls the file system, clock, environment and processes directly. Plugins join through seven `ServiceLoader` contracts, and the host blocks on every coroutine.

In 4.0:

- Every service gets its collaborators through its constructor.
- The host has three nested lifetimes: the app (one per JVM), the script run (one per script) and the generation run (one per `generate` call, with its own Metro graph).
- Plugins join through two contracts: `MicrosmithPlugin`, which is required, and `MicrosmithScriptingPlugin`, which is only for plugins that add script symbols.
- Each plugin has a stable id, builds its own graph from the host services it is given, and is closed when its generation run ends. No Metro type crosses the plugin boundary.
- A plugin declares only its `id`. Compatibility follows the semver of the plugin API module.
- A third-party plugin that uses Metro relocates its Metro runtime. An unrelocated one is unsupported. Hand-wiring is a fully supported alternative with no build set-up. Built-in plugins share the host's runtime.
- The script body runs on a dedicated thread that ends with the script run. After the run the host interrupts any thread that a plugin left running. A plugin must not leave a thread running, leave a thread local set or register a shutdown hook, because each keeps its class loader reachable.
- I/O, time and processes sit behind injected seams, so unit tests can replace them. Plugins receive five of them through `HostServices`.
- The host API is `suspend` from each composition root down, so cancellation reaches generation and child processes. The plugin API stays non-`suspend`.
- The host may call one plugin's services concurrently within a generation run. Plugin services must be stateless or thread-safe.
- The change ships with #203 and moves one module family at a time.

## The design at a glance

```mermaid
flowchart LR
    subgraph HostJar["Host: microsmith jars"]
        App["AppGraph<br/>one per JVM"]
        Script["Script run<br/>one per script"]
        Run["RunGraph<br/>one per generate call<br/>the generation run"]
        Pipe["Pipeline:<br/>resolve, contribute, assemble,<br/>compile, render, write"]
        App -->|creates| Run
        Script -.->|"owns the plugin class loader<br/>and passes it to"| Run
        Run -->|runs| Pipe
    end
    subgraph PluginJar["Plugin jar: loaded at run time"]
        Plugin["MicrosmithPlugin<br/>required"]
        SPlugin["MicrosmithScriptingPlugin<br/>optional"]
        PGraph["Plugin graph<br/>own Metro graph, or hand-wired"]
        Plugin -->|builds| PGraph
    end
    Run -.->|"ServiceLoader finds"| Plugin
    Script -.->|"ServiceLoader finds"| SPlugin
    Run -->|"services(host: HostServices)"| Plugin
    PGraph -->|"PluginServices:<br/>resolvers, compilers, ..."| Run
```

Only four microsmith entry interfaces cross between the two boxes. `MicrosmithPlugin`, `HostServices` and `PluginServices` are always used. `MicrosmithScriptingPlugin` is used only by a plugin that adds script symbols.

## Glossary

| Term | Meaning here |
|---|---|
| Service | An object that does work: a resolver, a renderer, a writer. Contrast with a value, such as `ResolvedProtobufMessage` or `GeneratedFile`. |
| Graph | A Metro `@DependencyGraph`: an interface from which Metro generates the code that builds services and their collaborators. |
| Binding | A rule in a graph that says how to get one type, such as an `@Inject` constructor or a `@Provides` function. |
| Scope | A marker type that ties an instance to a graph's lifetime. `@SingleIn(RunScope::class)` means one instance per `RunGraph`, which is one generation run. |
| Script run | One `ScriptRunRequest`: one script, compiled and evaluated once. It owns the plugin class loader and the script compile cache. It contains zero or more generation runs. |
| Generation run | One `generate` or `emit` call. It gets its own `RunGraph` and its own plugin graphs. |
| Graph extension | A Metro `@GraphExtension`: a child graph created from a parent. It sees the parent's bindings and lives for a shorter time. |
| Contribution | A Metro annotation such as `@ContributesTo` or `@ContributesIntoSet` that adds a binding to a graph from another module. Metro gathers contributions when it compiles the graph. |
| `@Includes` | A graph factory parameter whose properties become bindings. A plugin graph uses `@Includes host: HostServices` to see the host's services. |
| Assisted injection | A constructor that takes some arguments at call time and the rest from the graph. Metro generates a factory for it (`@AssistedInject`, `@AssistedFactory`). |
| Relocation | Shading a library's classes into another package inside a jar, so that the jar uses its own copy. A plugin that uses Metro relocates its Metro runtime. |
| Seam | An interface that stands between code and a call to the outside world, such as the file system. A test replaces the implementation. |
| Host binding | A binding in the host's graph that plugins cannot see, such as the coroutine dispatcher. |
| Structured concurrency | Every coroutine is a child of a parent. Cancelling the parent cancels the children, and the parent waits for them to finish. |
| Monotonic time | A clock that only moves forward and is used to measure how long something took. A wall clock can jump, so it must not be used for durations. |
| ServiceLoader | `java.util.ServiceLoader`: the JDK mechanism that finds implementations listed in a jar's `META-INF/services` files. |
| Composition root | The entry point that creates the first graph, such as `Cli.main`. |
| ABI | A module's public binary interface: what another jar can compile against. #207 checks it with ABI dumps. |
| Jimfs | An in-memory `java.nio.file.FileSystem` for tests. |

## Context

### Today versus 4.0

```mermaid
flowchart TB
    subgraph Today["Today"]
        direction TB
        SL1["ServiceLoader<br/>7 contracts"] -->|"public no-arg constructor"| PSR1["ProtobufSchemasResolver"]
        PSR1 -->|"new"| PDR1["ProtobufDeclarationResolver"]
        PDR1 -->|"new"| PRR1["ProtobufReferenceResolver"]
        PRR1 -->|"new"| PST1["ProtobufSymbolTable"]
        SL1 -->|"public no-arg constructor"| PFC1["ProtoFileArtifactCompiler"]
        PFC1 -->|"static call"| PFR1["object ProtobufFileRenderer"]
        GR1["MicrosmithGenerationRunner"] -->|"static call"| GPR1["object GenerationProgressReporter"]
        GPR1 -->|"println"| OUT1[("stdout")]
    end
    subgraph After["4.0"]
        direction TB
        SL2["ServiceLoader<br/>2 contracts"] --> PP2["ProtobufPlugin"]
        SL2 --> PS2["ProtobufScriptingPlugin"]
        PP2 -->|"creates"| PG2["ProtobufPluginGraph"]
        PG2 -->|"constructor"| PSR2["ProtobufSchemasResolver"]
        PSR2 -->|"injected factory"| PRF2["ProtobufReferenceResolver.Factory"]
        PG2 -->|"constructor"| PFC2["ProtoFileArtifactCompiler"]
        PFC2 -->|"injected"| PFR2["ProtobufFileRenderer"]
        PS2 -->|"holds"| PSC2["ProtobufScriptSymbolContributor"]
        GR2["MicrosmithGenerationRunner"] -->|"injected"| DS2["DiagnosticSink"]
    end
```

Composition happens in two ways today.

- `gen` uses Metro. `MicrosmithGenerationGraph` is an `internal` graph whose factory takes a `MicrosmithPluginCatalog`. `MicrosmithModel.generate` builds a new graph on every call. `verifyModuleLayers` fails the build if any other module applies Metro (`metroViolations` in `build-logic/.../layers/ModuleLayers.kt`).
- Every other module wires itself by hand. Plugins join through `ServiceLoader` over seven contracts: `discoverMicrosmithPlugins` loads six, and `ScriptSymbolContributorRegistry.discover` loads the seventh. Each implementation needs a public no-argument constructor.

This produces four problems.

### Problem 1: services build their own collaborators

A `ServiceLoader` plugin cannot receive collaborators, so it constructs them.

```kotlin
// resolve-schemas-protobuf: ProtobufSchemasResolver.resolve
val resolver = ProtobufDeclarationResolver(ProtobufReferenceResolver(ProtobufSymbolTable(symbols)))

// resolve-services-dotnet-asp: DotnetAspWorkspaceResolver
class DotnetAspWorkspaceResolver(
    private val dotnetWorkspaceResolver: DotnetWorkspaceResolver = DotnetWorkspaceResolver(),
) {
    private val restResolver = DotnetAspRestResolver()
```

`DotnetWorkspaceResolver()` is built by a default argument in three places: `DotnetWorkspaceDomainResolver`, `DotnetAspWorkspaceResolver` and `DotnetPackageWorkspaceResolver`.

### Problem 2: behaviour lives in `object`s

Main code has 84 top-level `object` declarations. About a dozen are Mappie mappers, which must be objects. Most of the rest are stateless services that a test cannot replace: `ProtobufFileRenderer`, `DotnetAspProjectRenderer`, `DotnetAspPortAllocator`, `GeneratedOutputPathResolver`, `GeneratedOutputUniquenessValidator`, `GenerationProgressReporter`, `ScriptEvaluator`, `ScriptContextFactory`, `PluginClassLoaderScope`.

### Problem 3: I/O, environment, clock and processes are called directly

About 70 of the 724 main source files do this. They cluster at the edges of the system.

| Module group | Files |
|---|---|
| `cli` | 41 |
| `runtime-scripting` | 11 |
| `gradle-plugin` | 6 |
| `gen` | 6 |
| All `resolve-*`, `artifact-*` and `compile-*` modules together | about 6 |

Typical calls:

| Where | Direct call |
|---|---|
| `GenerationProgressReporter` | `println("Generated all files in ...")` |
| `TemporaryDirectory` | `Files.createTempDirectory`, `System.currentTimeMillis()`, `System.err.println` |
| `ScriptHostPaths`, `PluginResolutionPaths` | `System.getenv`, `System.getProperty("user.home")`, `Path.of` |
| `CliDiagnosticEmitter`, `EventLogWriter` | `Instant.now()` |
| `ScriptEvaluator` | `System.nanoTime()` to time `host.eval` |
| `DefaultMicrosmithGradleWorkerProcessExecutor` | `ProcessBuilder(command).start()` |
| `MicrosmithGradleWorkerLauncher` | `System.getProperty("java.home")`, `System.getProperty("os.name")` |

### Problem 4: function parameters stand in for injection

`MicrosmithCli` takes 11 function-typed constructor parameters with defaults, such as `stdout: (String) -> Unit = ::println` and a `scriptRunner` that constructs `MicrosmithScriptHost()`. `RunCommandHandler` repeats the same `scriptRunner` default. The CLI is testable, but its wiring is duplicated and hidden in default arguments.

### The link to #203

#203 makes everything outside the intended API `internal`. Its dry run found 1,391 explicit-API warnings across 38 modules. A sample of 15 split 8 : 4 : 3 into intended API, should-be-internal, and public only because another module or `ServiceLoader` needs it. `BinaryFileArtifactAssembler` is in the last group. Once a plugin graph builds its services, they no longer need to be public. So the two changes decide visibility together.

## Goals and non-goals

| Goals | Non-goals |
|---|---|
| One rule for where logic lives, applied the same way in every module. | Plugins that replace host services, or inject each other's services. |
| Constructor injection for every service, checked at build time within each compilation unit. | A runtime container, reflection-based injection or a service locator. |
| I/O, clock, environment, console and processes behind injected seams that unit tests can replace. | Changing the pipeline stages, or the resolved-model and typed-failure rules in [Architecture principles](../architecture-principles.md). |
| A public plugin API made only of microsmith interfaces, with no Metro types. | A compatibility layer for 3.x plugins. |
| Lifetimes that are safe in long-lived JVMs (Maven, `mvnd`, the IDE) whose plugin classpaths change between builds. | A security sandbox for plugins. See [Trust model](#trust-model). |
| One lifecycle for plugins: a stable id, a fixed order, clear conflict and failure rules, and cleanup when a generation run ends. | Streaming results with `Flow`. The pipeline is a batch transform. |
| Cancellation that reaches generation and child processes. | |
| Visibility and injection decided together, family by family, with #203. | |

## Decision

### Where logic lives

Every operation has exactly one of four homes. Ask these questions in order.

```mermaid
flowchart TD
    Q1{"Do we own the type, and is<br/>the operation intrinsic to it?"}
    Q2{"Is it a step used<br/>inside one file only?"}
    Q3{"Does it have side effects, hold<br/>state, or need configuration?"}
    Q4{"Is it microsmith-specific, or does it<br/>transform one type into another?"}
    Q5{"Would the receiver's maintainers<br/>plausibly have written it?"}
    M["Member"]
    P["Private function"]
    S["Injected service or policy"]
    E["Extension"]
    Q1 -->|yes| M
    Q1 -->|no| Q2
    Q2 -->|yes| P
    Q2 -->|no| Q3
    Q3 -->|yes| S
    Q3 -->|no| Q4
    Q4 -->|yes| S
    Q4 -->|no| Q5
    Q5 -->|yes| E
    Q5 -->|no| S
```

| Home | Use when | Example |
|---|---|---|
| Member | We own the type and the operation is intrinsic to it. | `ResolvedProtobufMessage.of`; `ChangelogExtension.merge` in the [Plugin development](../plugin-development.md) example |
| Extension | A general operation the receiver's maintainers could have written. Never microsmith-specific, except DSL entry points (see [Exceptions](#exceptions-to-the-rules)). Never side-effecting. | `String.toSnakeCase()` |
| Injected service or policy | Microsmith-specific, side-effecting, stateful, configurable, or transforming between types. | Rendering a resolved message as C#; the .NET naming policy; `GeneratedOutputWriter` |
| Private function | A step inside one file. | `requireNoSymlinkTraversal` in `GeneratedOutputPathResolver` |

Four rules follow from the table.

**Rule 1: no `object` with behaviour, except Mappie mappers and script-definition objects.** `object` is still fine for constants, for `data object` values such as `ChangelogArtifactId` in the [Plugin development](../plugin-development.md) example, and for members of sealed hierarchies.

```kotlin
// Before
internal object ProtobufFileRenderer { fun render(artifact: ProtoFileArtifact): String = ... }

// After
@Inject internal class ProtobufFileRenderer { fun render(artifact: ProtoFileArtifact): String = ... }
```

**Rule 2: no service builds another service.** A service lists what it needs in its constructor.

```kotlin
// Before: compile-schemas-protobuf
@ServiceProvider(ArtifactCompiler::class)
class ProtoFileArtifactCompiler : SchemasArtifactCompiler<ProtoFileArtifact> {
    override fun compile(artifact: ProtoFileArtifact) =
        listOf(TextFileArtifactContribution(TextFileArtifactId(artifact.id.relativePath), ProtobufFileRenderer.render(artifact)))
}

// After
@Inject
internal class ProtoFileArtifactCompiler(private val renderer: ProtobufFileRenderer) : SchemasArtifactCompiler<ProtoFileArtifact> {
    override fun compile(artifact: ProtoFileArtifact) =
        listOf(TextFileArtifactContribution(TextFileArtifactId(artifact.id.relativePath), renderer.render(artifact)))
}
```

The compiler becomes `internal`, because the plugin graph builds it instead of `ServiceLoader`.

**Rule 3: values are constructed directly.** Injection is for services. `ResolvedProtobufMessage` and `GeneratedFile` are still built with their constructors or `of` functions.

**Rule 4: use a factory when construction needs both runtime values and services.** See [Factories](#factories).

#### Exceptions to the rules

There are two exceptions. Both are settled questions.

| Exception | Why | What it means |
|---|---|---|
| DSL entry points on scope types are extensions by design, such as `SchemasScope.protobuf` and `RpcScope`'s `String.to`. | A plugin adds vocabulary to a scope it does not own, and an extension is the only way to do that. Replacing them with members or registries is a large redesign outside this change. | The "never microsmith-specific" limit on extensions does not apply to them. |
| Script-definition objects, `MicrosmithScriptCompilationConfiguration` and `MicrosmithScriptEvaluationConfiguration`, may stay `object`s. | Kotlin scripting instantiates them, so they cannot take constructor injection. | They read the script symbol registry from a `ScriptingHostConfiguration` key that the host sets. If the key is absent, as in the IDE, the refiners fall back to discovering `MicrosmithScriptingPlugin` themselves. |

Further examples. Rule 1 and [Factories](#factories) show the other cases.

| Today | 4.0 |
|---|---|
| `internal object DotnetAspPortAllocator` | An injected service. Port allocation is a microsmith-specific policy. |
| `internal fun dotnetAspPascalIdentifier(identifier: String)` | A method on an injected .NET naming policy |
| `artifact.textContribution("Program.cs", ...)` in `compile-services-dotnet-asp` | A private function in the compiler's file, or a member of a contribution-builder service |

### Plugin API

The plugin API has two contracts. `MicrosmithPlugin` is required. `MicrosmithScriptingPlugin` is optional and exists only for plugins that add script symbols. None of the types mentions Metro.

```kotlin
public interface MicrosmithPlugin {
    public val id: String
    public fun services(host: HostServices): PluginServices
}

public interface HostServices {
    public val fileSystem: java.nio.file.FileSystem
    public val clock: Clock
    public val diagnostics: DiagnosticSink
    public val console: Console
    public val environment: Environment
}

public interface PluginServices : AutoCloseable {
    public val domainResolvers: Set<DomainResolver<*, *>> get() = emptySet()
    public val resolutionIssueDiagnosticMappers: Set<ResolutionIssueDiagnosticMapper<*>> get() = emptySet()
    public val artifactContributors: Set<ArtifactContributor<*>> get() = emptySet()
    public val artifactAssemblers: Set<ArtifactAssembler<*>> get() = emptySet()
    public val artifactCompilers: Set<ArtifactCompiler<*>> get() = emptySet()
    public val artifactRenderers: Set<ArtifactRenderer<*>> get() = emptySet()
    override fun close() {}
}
```

**Concurrency rule.** The host may call one plugin's services from several threads at once within a generation run, for example when it renders in parallel. Plugin services must be stateless or thread-safe within a generation run. A plugin author must not assume that calls are sequential.

The optional contract lives in `runtime-scripting-api`. Its name and shape here are illustrative.

```kotlin
public interface MicrosmithScriptingPlugin {
    public val id: String
    public val scriptSymbolContributors: Set<ScriptSymbolContributor>
}
```

Why scripting is a separate contract:

- Script symbols are needed at script compile time and in the IDE. Both happen before any `generate` call, and in the IDE no script run or generation run exists at all.
- So `MicrosmithScriptingPlugin` takes no `HostServices`. `ScriptSymbolContributor` implementations are pure: they take a declaration site and return values. `ProtobufScriptSymbolContributor`, the only one today, is pure.
- The base plugin API module has no dependency on `runtime-scripting-api`. A plugin that adds no script symbols never sees scripting types.
- A plugin that offers both should use the same `id` for both, so a diagnostic names one plugin.

What `ServiceLoader` finds, and what it needs:

- `ServiceLoader` finds `MicrosmithPlugin` and `MicrosmithScriptingPlugin`. The seven `META-INF/services` files in [Plugin development](../plugin-development.md#6-registration) become one, or two for a plugin with script symbols.
- `ServiceLoader` instantiates the registered provider class. So that class still needs a public no-argument constructor. Plugin jars are loaded from a class path, where `ServiceLoader` accepts nothing else: its static `provider()` alternative applies only to providers declared in a named module. Individual services, such as a resolver or a compiler, no longer need one, because the plugin builds them.
- A plugin may build its services with its own Metro graph, or wire them by hand. The host cannot tell the difference.

A plugin that uses Metro:

```kotlin
@DependencyGraph(ProtobufPluginScope::class)
internal interface ProtobufPluginGraph : PluginServices {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Includes host: HostServices): ProtobufPluginGraph
    }
}

public class ProtobufPlugin : MicrosmithPlugin {
    override val id: String = "io.github.lmliam.microsmith.protobuf"

    override fun services(host: HostServices): PluginServices =
        createGraphFactory<ProtobufPluginGraph.Factory>().create(host)
}
```

The same plugin, with script symbols (illustrative):

```kotlin
public class ProtobufScriptingPlugin : MicrosmithScriptingPlugin {
    override val id: String = "io.github.lmliam.microsmith.protobuf"
    override val scriptSymbolContributors: Set<ScriptSymbolContributor> = setOf(ProtobufScriptSymbolContributor())
}
```

**Why each plugin has a standalone graph.** Metro gathers contributions when it compiles a graph. So a jar loaded at run time cannot contribute to the host graph. A graph extension also cannot be compiled apart from its parent. The [graph-boundary spike](#spike-results) confirms both.

**Limits we accept.**

- A plugin cannot replace a host service.
- Plugins cannot inject each other's services. They meet only through pipeline models: resolved models, artifacts and contributions.
- Plugin services must be stateless or thread-safe, because the host may call them concurrently within a generation run. A plugin that needs state must synchronise it. Option a, never calling a plugin concurrently, was rejected: it would make rendering sequential, and tightening the contract after 4.0 would break plugins.

#### Built-in plugins

There is one built-in plugin per module family: protobuf, protobuf RPC, .NET, ASP.NET and .NET packages. Each family's graph builds `DotnetWorkspaceResolver` as a library class, because plugins cannot inject each other's services. This matches the module families and how third parties will ship. Each family already resolves the .NET workspace on its own today.

#### Where the plugin API lives

`PluginServices` names `ArtifactRenderer`, which is in `gen` today. A plugin should not need the engine on its compile classpath. So `ArtifactRenderer` and `GeneratedFile` move down to a layer below `gen`, in the same way that `DomainResolver` lives in `resolve`. `gen` keeps the implementations. The target module must be able to see the relative-path type in `artifact` (see [Paths in pipeline models](#paths-in-pipeline-models)). Slice 0 picks it.

```mermaid
flowchart BT
    Core["resolve, artifact, compile<br/>plus ArtifactRenderer, GeneratedFile"] --> Api["plugin API<br/>MicrosmithPlugin, HostServices, PluginServices"]
    Api --> Plugin["third-party plugin"]
    Api --> Gen["gen"]
    Rsa["runtime-scripting-api<br/>MicrosmithScriptingPlugin"] -.->|"only for script symbols"| Plugin
```

Arrows point from a dependency to the module that uses it.

#### `HostServices` is a per-plugin view

The host builds a new `HostServices` for each plugin in each generation run. The five members come from two places.

```mermaid
flowchart LR
    subgraph Shared["App scope: shared and thread-safe"]
        FS["FileSystem"]
        CK["Clock"]
        EN["Environment"]
        CO["Console"]
        EM["Diagnostic emitter"]
    end
    subgraph GenRun["One generation run"]
        HA["HostServices for plugin A<br/>own DiagnosticSink, tagged A"]
        HB["HostServices for plugin B<br/>own DiagnosticSink, tagged B"]
    end
    HA -->|"fileSystem, clock, environment,<br/>console delegate"| Shared
    HB -->|"fileSystem, clock, environment,<br/>console delegate"| Shared
    HA -->|"forwards diagnostics"| EM
    HB -->|"forwards diagnostics"| EM
```

| Member | What the plugin gets |
|---|---|
| `fileSystem`, `clock`, `environment`, `console` | A delegate to the app-scoped implementation. |
| `diagnostics` | A sink scoped to the generation run. It tags every diagnostic with the plugin id and forwards it to the app-level emitter. |

**Concurrency rule.** Maven can run several generation runs at once in one JVM. So every app-scoped seam implementation must be thread-safe. `Console` writes must be atomic per line or message. Today `CliDiagnosticEmitter` has no mutable state and no synchronisation around its sinks, and `GenerationProgressReporter` is a stateless `object` that calls `println`. Progress state, buffers and diagnostic context are generation-run state. They never live in an app singleton.

#### Metro across the plugin boundary

No Metro type crosses the boundary, so a plugin may use Metro or not. But a plugin that uses Metro carries a coupling that the plugin API cannot hide, unless the plugin relocates its Metro runtime.

| Plugin kind | Which Metro runtime runs | What follows |
|---|---|---|
| Built-in | The host's. It is compiled with the host. | Always matches. |
| Third-party, uses Metro, relocated | Its own, under a relocated package name. | No coupling to the host's Metro version. Supported. |
| Third-party, uses Metro, unrelocated | The host's. Plugin class loaders are parent-first, so a Metro runtime bundled unrelocated in the plugin jar is ignored. | Unsupported. It may work, but only by luck. When it fails, the host reports an incompatible plugin. |
| Third-party, hand-wired | None. | No coupling. Supported, with no build set-up. |

Metro does not guarantee that generated code is compatible with a runtime version other than the one it was compiled against (see [Metro's stability page](https://zacsweers.github.io/metro/latest/stability)). So a microsmith release that bumps Metro can break an unrelocated third-party Metro plugin without any change to the microsmith plugin API. A mismatch shows up as a `LinkageError` inside `services(host)`, such as the `NoSuchMethodError` that the [spike](#spike-relocated-metro-runtime) saw. The host reports it as described in [Plugin identity, ordering, conflicts and failure](#plugin-identity-ordering-conflicts-and-failure).

**Policy for third-party plugins.** A third-party plugin that uses Metro relocates (shades) its own Metro runtime, so it is self-contained. This is safe by design, because no Metro type crosses the boundary. The [spike](#spike-relocated-metro-runtime) showed that a relocated plugin built with any of five Metro versions runs against a host on Metro 1.5.2.

- An unrelocated third-party Metro plugin is unsupported.
- Hand-wiring is a fully supported alternative. It needs no build set-up.
- Built-in plugins share the host's Metro runtime.
- Slice 5 rewrites [Plugin development](../plugin-development.md) to document relocation.

A plugin author configures the build like this and ships the `shadowJar` output:

```kotlin
plugins {
    kotlin("jvm")
    id("dev.zacsweers.metro")
    id("com.gradleup.shadow") version "9.6.1"
}

tasks.shadowJar {
    relocate("dev.zacsweers.metro", "com.example.myplugin.shaded.metro")
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains:annotations"))
    }
}
```

The plugin must not shade the Kotlin standard library. The Metro runtime depends only on `kotlin-stdlib`, and the host's serves it.

Asking authors to compile against the exact Metro version of each release was rejected. It breaks plugins on a minor release, and it needs release notes that flag every Metro bump.

### Host composition

The host has three levels. The app graph and the generation run graph are Metro graphs in `gen`. The script run lives in `runtime-scripting`.

```mermaid
flowchart TB
    Root["Composition roots:<br/>Cli.main, MicrosmithGenerateMojo.execute,<br/>MicrosmithGradleWorkerMain, ProcessIsolatedScriptMain"]
    subgraph HostSide["Host jars, compiled together"]
        subgraph AppG["AppGraph: DependencyGraph of AppScope, one per JVM, in gen"]
            HS["HostServices implementations, the seams"]
            SCP["Cache directory and fingerprint policy"]
            HB["Host bindings: dispatcher, time source"]
        end
        subgraph ScriptG["Script run graph: one per ScriptRunRequest, in runtime-scripting"]
            CL["Plugin class loader"]
            SC["MicrosmithScriptCache"]
            CTX["MicrosmithScriptContext"]
            SYM["Script symbol registry"]
        end
        subgraph RunG["RunGraph: GraphExtension of RunScope, one per generate call, in gen"]
            PD["Plugin discovery, sorted by id"]
            BI["Built-in renderers and assemblers"]
            CAT["Plugin catalog"]
            SVC["DomainResolutionService, ArtifactContributionService,<br/>ArtifactAssemblyService, ArtifactCompilationService,<br/>ArtifactRenderingService, GeneratedOutputWriter"]
            RUN["MicrosmithGenerationRunner"]
            PD --> CAT
            BI --> CAT
            CAT --> SVC --> RUN
        end
        AppG -->|"exposed services build"| ScriptG
        AppG -->|"generate creates"| RunG
        ScriptG -.->|"passes the plugin class loader"| RunG
    end
    subgraph PluginSide["Plugin jars, compiled separately"]
        P1["protobuf graph"]
        P2["dotnet-asp graph"]
        P3["third-party graph"]
    end
    Root -->|"passes seam implementations"| AppG
    PD -.->|"ServiceLoader: MicrosmithPlugin"| PluginSide
    SYM -.->|"ServiceLoader: MicrosmithScriptingPlugin"| PluginSide
    HS -->|"services(host)"| PluginSide
    PluginSide -->|"PluginServices"| CAT
```

- **`AppGraph`** replaces `MicrosmithGenerationGraph`. The composition roots create it: `Cli.main`, `MicrosmithGradleWorkerMain`, `ProcessIsolatedScriptMain` and `MicrosmithGenerateMojo.execute`. It holds only immutable, class-loader-independent state.
- **The script run** is built in `runtime-scripting`, from the services that the app graph exposes. It owns the plugin class loader. It exists before compilation, so the script compile cache is available to the compiler.
- **`RunGraph`** is a `@GraphExtension(RunScope::class)` of `AppGraph`. Its factory takes the plugin class loader. It discovers `MicrosmithPlugin` implementations, sorts them by id, calls `services(host)` on each, and merges the results into the catalog.
- **A generation run does not need a script run.** `gen` must stay usable without scripting. A programmatic `generate` call with no script uses the caller's class loader.
- **Built-ins are bound, not discovered.** `RunGraph` binds `TextFileArtifactRenderer`, `BinaryFileArtifactRenderer`, `TextFileArtifactAssembler` and `BinaryFileArtifactAssembler` directly.
- **The layer rule changes.** "Metro only in gen" is replaced by two rules:
  1. No published API module (`dsl`, `resolve`, `artifact`, `compile`, the plugin API module, `runtime-scripting-api`) exposes a Metro type in its ABI. The ABI dumps from #207, which ship in 3.0.0, can check this.
  2. Metro scope markers used on both sides of the boundary live in the plugin API module.

**One constraint.** Metro aggregates bindings when the module that declares a graph compiles. So `gen`'s graph cannot see bindings declared in `runtime-scripting` or `cli`. Two things follow.

1. The composition root passes seam implementations into the app graph's factory. For example, the CLI passes its `Console`.
2. The script run graph in `runtime-scripting` is built from the app graph's exposed services. It is not a child of the app graph in `gen`.

Slice 0 confirms whether the script run graph is a graph extension or an `@Includes` graph.

### One `generate` call

A script run may contain several of these. A programmatic caller has no script run and calls `generate` directly.

```mermaid
sequenceDiagram
    participant Root as Composition root
    participant Script as Script run
    participant App as AppGraph
    participant Run as RunGraph
    participant SL as ServiceLoader
    participant Plugin as MicrosmithPlugin
    participant PG as Plugin graph
    participant Runner as MicrosmithGenerationRunner
    Root->>App: create, once per JVM
    Root->>Script: run(request)
    Note over Script: open plugin class loader,<br/>create cache, context and symbol registry
    Script->>App: generate(model, pluginClassLoader)
    App->>Run: create(pluginClassLoader)
    Run->>SL: load MicrosmithPlugin
    SL-->>Run: plugins
    Note over Run: sort by id, check for duplicate ids
    loop each plugin in id order
        Run->>Plugin: services(host)
        Plugin->>PG: create(host)
        PG-->>Run: PluginServices
    end
    Run->>Runner: generate(model, outputDir)
    Note over Runner: resolve, contribute, assemble,<br/>compile, render, write
    Runner-->>Script: generated paths
    Note over Run,PG: close PluginServices, then discard RunGraph and plugin graphs
    Note over Script: the script may call generate again,<br/>each call is a new generation run
    Note over Script: at the end, close the plugin class loader
```

### Lifetimes

There are three named lifetimes, and they nest in time. A fourth, transient, is the default for everything else.

| Lifetime | Scope | Holds | Rule |
|---|---|---|---|
| App singleton | `AppScope` | Seam implementations, cache directory, fingerprint policy, host bindings | Immutable state only, and nothing that depends on the plugin class loader. Nothing to close. |
| Script run | A graph in `runtime-scripting` | Plugin class loader, `MicrosmithScriptCache`, script context, script symbol registry | One `ScriptRunRequest`. Exists before compilation. Closes the class loader after its last generation run closes. |
| Generation run | `RunScope`, and each plugin graph's own scope | `RunGraph`, plugin graphs, pipeline services, run-scoped diagnostics state | One `generate` or `emit` call. A plugin's `@SingleIn` means once per generation run. |
| Transient | none | Most services | A new instance per injection. The default. |

```mermaid
flowchart TB
    subgraph AppL["App: one per JVM"]
        AppOwns["Owns: seam implementations,<br/>cache directory, fingerprint policy"]
        subgraph SRL["Script run: one ScriptRunRequest"]
            SROwns["Owns: plugin class loader, MicrosmithScriptCache,<br/>script context, script symbol registry"]
            subgraph G1["Generation run 1: first generate call"]
                G1Owns["Owns: RunGraph, plugin graphs,<br/>pipeline services, diagnostics state"]
            end
            subgraph G2["Generation run 2: second generate call"]
                G2Owns["Owns: RunGraph, plugin graphs,<br/>pipeline services, diagnostics state"]
            end
            G1Owns -->|"closed before"| G2Owns
        end
    end
```

The levels nest in time, not in the graph. `RunGraph` is created from the app graph by `generate`. The script run owns the plugin class loader and passes it to each generation run.

**Why the script run is its own lifetime.** `ClassloaderScriptRunner.execute` runs once per `ScriptRunRequest`. In that one call it does these things in order.

1. It opens the plugin class loader (`PluginClassLoaderScope`: a `URLClassLoader` that `use` closes when the block returns, or the host's own class loader when the request has no plugin classpath).
2. It constructs `MicrosmithScriptCache`.
3. It creates the script context (`ScriptContextFactory.create`).
4. It compiles and evaluates the script.

The Kotlin scripting host reads and writes the cache during `host.eval`. That happens before, and independent of, any `generate` call. `generate` never touches the cache. So the cache cannot be generation-run state. A script can call `emit` or `generate` more than once, because `MicrosmithScriptContext.emit` accumulates `generatedRoots`. Each call is its own generation run. A script that calls `generate` three times gets three generation runs inside one script run.

The graph-boundary spike observed this across two generation runs in one JVM:

```mermaid
sequenceDiagram
    participant App as AppGraph
    participant R1 as RunGraph generation run 1
    participant P1 as Plugin graph run 1
    participant R2 as RunGraph generation run 2
    participant P2 as Plugin graph run 2
    Note over App: App singleton created once
    App->>R1: create
    R1->>P1: services(host)
    Note over P1: SingleIn PluginScope resolver A,<br/>the same instance throughout generation run 1
    App->>R2: create
    R2->>P2: services(host)
    Note over P2: resolver B, a new instance
    Note over App,P2: Both generation runs received the same app singleton
```

**Why app singletons are restricted.** One JVM can run many builds with different plugin classpaths. An app singleton that held a plugin class would leak its class loader and could hand a stale class to the next build.

Where a JVM is reused today:

| Host | Where generation runs | Consequence |
|---|---|---|
| Maven | In the build JVM. `MicrosmithGenerateMojo` is `threadSafe = true`. | A parallel build can run several generation runs at once, so every app-scoped seam must be thread-safe. |
| IDE | The IDE process loads `MicrosmithScriptCompilationConfiguration`, whose refiner calls `ScriptSymbolContributorRegistry.discover()`. | Plugin code runs in a long-lived JVM. No script run or generation run exists, so the refiners discover `MicrosmithScriptingPlugin` themselves. |
| Gradle | In a new JVM for every task execution, started by `MicrosmithGradleWorkerLauncher`. | Not the case that drives the rule, but the rule still holds. |

A lifetime says how long an object lives. It does not say how its resources are released. That is covered in [Lifecycle and disposal](#lifecycle-and-disposal).

### I/O seams

Each seam is its own interface on `HostServices`. There are five.

| Seam | Replaces | Main users today |
|---|---|---|
| `java.nio.file.FileSystem` | `Path.of`, `Paths.get`, and the default file system behind `Files.*` | `gen` output writing, `cli` plugin cache and lockfiles, `ScriptHostPaths` |
| `Clock` (`kotlin.time.Clock`) | `Instant.now()`, `System.currentTimeMillis()` | `CliDiagnosticEmitter`, `EventLogWriter`, `TemporaryDirectory` |
| `DiagnosticSink` | Ad hoc warnings and progress on stdout or stderr | `GenerationProgressReporter`, `TemporaryDirectory.close` |
| `Console` | `println`, `System.err.println`, and `MicrosmithCli`'s `stdout` and `stderr` lambdas | `cli`, `ProcessIsolatedScriptMain`, `MicrosmithGradleWorkerMain` |
| `Environment` | `System.getenv`, `System.getProperty` | `ScriptHostPaths`, `PluginResolutionPaths`, `DoctorPaths`, `RepositoryAllowlistPolicy`, `DefaultRepositoryCredentialsResolver`, `MicrosmithGradleWorkerLauncher` |

Three more bindings exist, but they are host-only. They are not on `HostServices` in 4.0.

| Host binding | Replaces | Main users today |
|---|---|---|
| `TimeSource` (`kotlin.time.TimeSource`) | `System.nanoTime()` | `ScriptEvaluator` |
| Coroutine dispatcher | The `Dispatchers.IO` default argument on `GeneratedOutputWriter`, `TemporaryDirectory.create` and `generateTo` | The generation pipeline |
| `ProcessLauncher` (`suspend`) | `ProcessBuilder` | `DefaultMicrosmithGradleWorkerProcessExecutor`, `JvmProcessIsolationWorkerLauncher` |

**Why these three stay host-only.** `HostServices` is implemented by the host and consumed by plugins. So a member can be added later without breaking plugins, but removing one is a break. A member that plugins have no need for should not be added until one does. No plugin launches a process today: both users of `ProcessBuilder` are host code. `ProcessLauncher` is also `suspend`, and no function in the plugin API is, so a plugin could not call it.

```kotlin
// Before: ScriptHostPaths
val envPath = System.getenv("MICROSMITH_SCRIPT_CACHE_DIR")?.trim()?.takeIf { it.isNotEmpty() }
return Path.of(System.getProperty("user.home"), ".microsmith", "cache", "scripts")

// After (Environment method names are illustrative)
val envPath = environment.variable("MICROSMITH_SCRIPT_CACHE_DIR")?.trim()?.takeIf { it.isNotEmpty() }
return fileSystem.getPath(environment.property("user.home"), ".microsmith", "cache", "scripts")
```

`Path.of` and `Paths.get` are banned in main code in favour of `fileSystem.getPath`. A build check enforces the ban.

Some APIs accept only the default file system: Kotlin scripting (`FileScriptSource`, `CompiledScriptJarsCache`, and `updateClasspath`, which take `java.io.File`) and `URLClassLoader`. Code that calls them stays on the default file system, and integration tests cover it.

#### Time: two seams

Time has two jobs, and each gets its own type.

| Job | Type | Production | Tests |
|---|---|---|---|
| "What time is it?" A timestamp in an event or a name. | `kotlin.time.Clock`, stable since Kotlin 2.3. The build uses 2.4.21. | `Clock.System` | A fixed or controllable clock |
| "How long did it take?" A duration. | `kotlin.time.TimeSource` | `TimeSource.Monotonic` | `TestTimeSource` |

A wall clock must not measure durations, because it can jump when the system clock changes. `System.nanoTime()` in `ScriptEvaluator` is the only duration measurement today, and its value is reported, never used for control flow.

```kotlin
// Before: ScriptEvaluator
val startNanos = System.nanoTime()
val result = host.eval(...)
val elapsedMillis = (System.nanoTime() - startNanos) / NANOS_PER_MILLISECOND

// After (illustrative)
val mark = timeSource.markNow()
val result = host.eval(...)
val elapsed = mark.elapsedNow()
```

`kotlin.time.Clock` also settles [question 9](#settled-questions): it replaces `Instant.now()` and `System.currentTimeMillis()`.

#### Paths in pipeline models

Pipeline models hold `java.nio.file.Path` in six types today. The first three also carry a relative path.

| Type | Module | Members that hold a `Path` |
|---|---|---|
| `TextFileArtifactId` | `artifact` | `relativePath`, `outputRoot` |
| `BinaryFileArtifactId` | `artifact` | `relativePath`, `outputRoot` |
| `GeneratedFile` | `gen` | `relativePath`, `outputRoot` (default `Path.of(".")`) |
| `ResolvedDotnetAspService` | `resolve-services-dotnet-asp` | `outputRoot` |
| `DotnetAspServiceArtifact` | `artifact-services-dotnet-asp` | `outputRoot` |
| `DotnetAspServiceContribution` | `artifact-services-dotnet-asp` | `outputRoot` |

Pure modules build these paths with `Path.of`: `ProtoFileArtifactId`, `MsBuildProjectArtifactCompiler` and `DotnetAspProjectFileSupport`. They have no `FileSystem`. A default-file-system `Path` cannot be resolved against a Jimfs root (`ProviderMismatchException`). The `Path.of` ban would also leave these modules no way to build a path.

The decision: pipeline models carry a file-system-independent relative-path value type, defined in `artifact`. `outputRoot` becomes the same logical type. `GeneratedOutputWriter` resolves both against the injected `FileSystem` at the write boundary.

```mermaid
flowchart LR
    Model["Pipeline models<br/>logical relative path<br/>and logical output root"] --> Writer["GeneratedOutputWriter<br/>resolves against the injected FileSystem"]
    Writer --> Disk[("Files on disk<br/>or in Jimfs")]
```

```kotlin
// Before
TextFileArtifactId(Path.of("schemas", "orders.proto"))

// After (the type name is illustrative)
TextFileArtifactId(RelativePath.of("schemas", "orders.proto"))
```

This is a 4.0 breaking change to the six types above. The infix `Path.to(contents)` helper in `GeneratedFile.kt` changes its receiver with them. 4.0 already breaks the plugin API, and pipeline models should not depend on a file system.

### Factories

Use assisted injection when a service needs a value known only at call time as well as injected services.

```kotlin
// Before: ProtobufSchemasResolver.resolve
val resolver = ProtobufDeclarationResolver(ProtobufReferenceResolver(ProtobufSymbolTable(symbols)))

// After
@AssistedInject
internal class ProtobufReferenceResolver(
    @Assisted private val symbols: ProtobufSymbolTable,
    private val naming: ProtobufNamingPolicy,
) {
    @AssistedFactory
    fun interface Factory {
        fun create(symbols: ProtobufSymbolTable): ProtobufReferenceResolver
    }
}
```

Candidates in today's code:

| Type | Value known at call time | Injected services | Built once per |
|---|---|---|---|
| `ProtobufReferenceResolver`, `ProtobufDeclarationResolver` | The symbol table built in `ProtobufSchemasResolver.resolve` | Naming and validation policies | Call |
| `TemporaryDirectory` (today `TemporaryDirectory.create`) | A prefix | File system, clock | Generation run |
| `MicrosmithScriptCache` | The cache directory and the plugin classpath fingerprint | File system | Script run |
| `MicrosmithScriptContext` (today `ScriptContextFactory.create`) | The output path and the request | The generation entry point | Script run |

Do not use a factory for a value. `DirectorySpace.from(path)` builds a value but also calls `Files.createDirectories`. The side effect moves into a service, and `DirectorySpace` keeps a plain constructor.

### Testing

| Layer | File system | Clock, time source, process, environment |
|---|---|---|
| Unit | May inject an in-memory file system (Jimfs) through the injected `FileSystem`. | Injected fakes, such as `TestTimeSource`. |
| Integration and end-to-end | Real temporary directories, to catch real permissions, symbolic links and OS paths. For example, `GeneratedOutputPathResolver`'s symbolic-link checks need a real file system. | Real, unless the test is about them. |

Tests use a plugin's real graph or hand-wired real collaborators. They do not use a test container. Tests of `suspend` code use `runTest` with a test dispatcher bound in place of the host's dispatcher (see [Coroutines and `suspend`](#coroutines-and-suspend)). The acceptance tests that slice 0 adds are listed under [Acceptance tests for slice 0](#acceptance-tests-for-slice-0).

Changes to `AGENTS.md`:

| Line today | Becomes |
|---|---|
| Unit layer row: "A temporary directory is allowed when the unit writes files." | "An in-memory file system is allowed when the unit reads or writes files." |
| "Use a test double only at a boundary: network, file system, clock." | "Use a test double only at a boundary: network, file system, clock, process, environment." |
| "Use a temporary directory instead of a fake file system." | "In unit tests, use an in-memory file system through the injected `FileSystem`. In integration and end-to-end tests, use a real temporary directory." |

### Plugin identity, ordering, conflicts and failure

#### Identity

`MicrosmithPlugin` has a `public val id: String`. The id is stable, unique and reverse-DNS style, such as `io.github.lmliam.microsmith.protobuf`. The host uses it to sort plugins and to name them in diagnostics.

A plugin declares only its `id`. It does not declare a `version` or an `apiVersion`. Compatibility is defined by the semver of the plugin API module (see [Compatibility expectation](#compatibility-expectation)). A declared `apiVersion` was not added, because the host reads it by instantiating the class, and that is the step that fails when the API is incompatible. So it could not give the clear error it promises. A check that works would have to read jar metadata without loading classes. It can be added later without breaking plugins.

#### Order

The host sorts discovered plugins by `id` before it calls `services`. Generated output must never depend on `ServiceLoader` order, class path order or `Set` iteration order.

Today `discoverMicrosmithPlugins` keeps the `ServiceLoader` iteration order. The output is still stable, because the stages that follow sort. 4.0 keeps all of this and adds the sort by id.

| Component | What it sorts |
|---|---|
| `DomainResolverRegistry`, `DomainResolutionService` | Resolvers, by resolved type and class name |
| `ArtifactContributorRegistry`, `ArtifactContributionService` | Contributors and their results |
| `ResolutionIssueDiagnosticMapperRegistry` | Mappers |
| `ScriptSymbolContributorRegistry` | Contributors, by id |
| `MicrosmithGenerationRunner` | Output roots |
| Host, new in 4.0 | Plugins, by id, before `services(host)` |

Slice 0 adds a test that shuffles the discovery order and expects byte-identical output.

#### Conflicts

When two plugins collide, the generation run fails with an error that names both plugin ids. The host records which plugin supplied each service, so it can do this.

| Situation | What happens |
|---|---|
| Two plugins have the same `id` | The generation run fails. |
| Two compilers, assemblers or renderers claim one artifact type | The generation run fails. This is today's rule: `ArtifactCompilerRegistry`, `ArtifactAssemblerRegistry` and `ArtifactRendererRegistry` fail at construction. |
| Several resolvers for one authoring type | All run, in sorted order. This is today's rule. `DomainResolverRegistry` rejects only the same class registered twice for one type. |
| Several contributions to one artifact id | The assembler merges them. The text and binary assemblers accept identical content and reject differing content. This is today's rule. |
| Two outputs with the same path | The generation run fails. This is today's rule: `GeneratedOutputUniquenessValidator` rejects two outputs with the same normalised root and path. |

#### Failure

If discovery of a plugin, or its `services(host)` call, throws, the whole generation run fails before the pipeline starts. This includes `ServiceConfigurationError` and `LinkageError`.

```mermaid
flowchart TB
    D["Discover plugins"] --> S["Sort by id and reject duplicate ids"]
    S --> SV["Call services(host) on each plugin"]
    SV --> M["Merge into the catalog and check conflicts"]
    M --> P["Run the pipeline"]
    D -->|"throws"| F["The generation run fails before the pipeline starts<br/>with a diagnostic that names the plugin"]
    S -->|"duplicate id"| F
    SV -->|"throws"| F
    M -->|"conflict"| F
```

The diagnostic names the plugin id when it is known. Otherwise it names the provider class and the jar. There is no partial loading. Silently skipping a plugin would silently change the output.

Today `loadServices` does not catch anything. A broken plugin surfaces as a `ServiceConfigurationError`, reported as "Unhandled script host failure: ..." or "Failed to load runtime service providers: ...". Nothing guarantees that the message names the plugin.

#### Compatibility expectation

The plugin API follows semver, and the ABI dumps from #207 check it. A plugin built against 4.x runs on any later 4.y. This is the whole compatibility contract: there is no declared `apiVersion`. A `LinkageError` at the boundary is reported as an incompatible plugin, not as a raw stack trace. A third-party plugin that uses Metro must also relocate its Metro runtime: see [Metro across the plugin boundary](#metro-across-the-plugin-boundary).

### Coroutines and `suspend`

#### Coroutines today

```mermaid
flowchart TB
    subgraph Entry["Entry points: plain blocking functions"]
        Cli["Cli.main"] --> RCH["RunCommandHandler"]
        Mojo["MicrosmithGenerateMojo.execute"] --> DR["DefaultMicrosmithScriptHostRunner"]
        GW["MicrosmithGradleWorkerMain"]
    end
    RCH --> Host["MicrosmithScriptHost.run"]
    DR --> Host
    GW --> Host
    PI["ProcessIsolatedScriptMain"] --> Exec
    Host --> Exec["ScriptRunExecutor"]
    Exec --> CSR["ClassloaderScriptRunner"]
    CSR --> Eval["ScriptEvaluator<br/>host.eval"]
    Eval --> Body["Script body<br/>calls emit or generate"]
    Body --> RB["ScriptContextFactory<br/>runBlocking, once per call"]
    subgraph Suspend["suspend, but every stage is sequential"]
        Gen["MicrosmithModel.generateTo"] --> St["Resolve, contribute, assemble,<br/>compile, render"]
        St --> Wr["GeneratedOutputWriter<br/>forEach on Dispatchers.IO"]
    end
    RB --> Gen
    CSR --> Proc["JvmProcessIsolationWorkerLauncher<br/>ProcessBuilder, read to EOF, blocking waitFor"]
```

What this shows:

- `MicrosmithGenerationRunner.generate`, `MicrosmithModel.generate` and `generateTo`, `TemporaryDirectory.create` and `GeneratedOutputWriter.write` are `suspend`. `generateTo`, `TemporaryDirectory.create` and `GeneratedOutputWriter.write` each call `withContext(Dispatchers.IO)`.
- The only `runBlocking` in main code is in `ScriptContextFactory`: `runBlocking { model.generateTo(outputPath) }`. It runs on the script's thread, once per `emit` or `generate` call. Everything above it is blocking.
- There is no `async`, `launch`, `coroutineScope` or `Flow` in main code. Every pipeline stage is sequential: `DomainResolutionService`, `ArtifactContributionService`, `ArtifactAssemblyService`, `ArtifactCompilationService`, `ArtifactRenderingService`, and `GeneratedOutputWriter`, which does a sequential `forEach` on the I/O dispatcher.
- `JvmProcessIsolationWorkerLauncher` and `DefaultMicrosmithGradleWorkerProcessExecutor` start a `ProcessBuilder`, read stdout to the end, then call a blocking `waitFor()`. There is no timeout, no `destroy` and no cancellation.
- None of the seven plugin contracts is `suspend`.

**The honest finding.** Today `suspend` only moves I/O to another thread. There is no concurrency. There is no way to cancel a process. The dispatcher is chosen by a default argument.

#### Options

| Option | What changes | For | Against |
|---|---|---|---|
| a. Keep as is, and move the dispatcher to a host binding | Only where the dispatcher comes from. | Smallest change. | Keeps blocking entry points, uncancellable processes and no concurrency. `suspend` stays a thread switch. |
| b. Structured concurrency in the host. Plugin contracts stay blocking. | The host API is `suspend` from each composition root down. Processes are cancellable. Independent I/O runs in parallel. | Cancellation and timeouts work. Plugins are unaffected. The cost lands in the host, where one team owns it. | Slices 4 and 5 touch many call sites. The host has thread-safety obligations. |
| c. `suspend` everywhere, including the plugin API | As b, plus every plugin API function becomes `suspend`. | Plugins could await I/O. | Every contract is a pure CPU transform today. `suspend` would colour every plugin and add nothing. It is a breaking change for every plugin author. |

#### Decision: option b

1. **The host API is `suspend` from the composition roots down.** `MicrosmithScriptHost.run` and the chain beneath it become `suspend`. Each composition root holds exactly one `runBlocking`: `Cli.main`, `MicrosmithGenerateMojo.execute`, `MicrosmithGradleWorkerMain` and `ProcessIsolatedScriptMain`.
2. **One bridge remains inside.** A Kotlin script body is not `suspend`, so `MicrosmithScriptContext.generate` and `emit` must block. The bridge runs the generation as a child of the script run's `Job`. Cancelling the script run then cancels the generation.

   ```kotlin
   // Before: ScriptContextFactory
   { model -> runBlocking { model.generateTo(outputPath) } }

   // After (illustrative)
   { model -> runBlocking(scriptRunJob) { model.generateTo(outputPath) } }
   ```

   The hazard: a bare nested `runBlocking` is not a child of anything, so cancellation would never reach it. The script body itself cannot be interrupted. A cancelled script stops at its next `emit` or `generate` call. The script body runs on a dedicated thread that ends with the script run, so a blocking script never occupies the root's event loop. It does not run on a pooled I/O dispatcher thread. Pooled threads are long-lived, so a plugin's thread local on one would keep the plugin class loader reachable (see [Lifecycle and disposal](#lifecycle-and-disposal)). The generation work that the script starts may still use the bound dispatcher.
3. **`ProcessLauncher` is a `suspend` host binding.** Awaiting a process is cancellable. Cancellation destroys the process. Callers can apply `withTimeout`. This fixes today's blocking `waitFor()` with no timeout.

   ```kotlin
   // Before: JvmProcessIsolationWorkerLauncher.execute
   val process = ProcessBuilder(buildCommand(requestFile, resultFile)).redirectErrorStream(true).start()
   val processOutput = process.inputStream.bufferedReader().use { it.readText().trim() }
   val exitCode = process.waitFor()

   // After (illustrative)
   public interface ProcessLauncher {
       public suspend fun run(command: List<String>): ProcessResult
   }

   // The caller sets a time limit. Cancelling the call destroys the process.
   val result = withTimeout(10.minutes) { processLauncher.run(command) }
   ```
4. **The plugin API stays non-`suspend` in 4.0.** Every service interface is a pure CPU transform, and `services(host)` only builds objects. `suspend` would colour every plugin and add nothing. The host checks for cancellation between units of work: between domains, between artifacts and between files.
5. **The dispatcher is a host binding** (settled question 8). Choosing a dispatcher is separate from being `suspend`. Code is `suspend` where it awaits something or must be cancellable. It is not `suspend` merely to reach `Dispatchers.IO`.
6. **Parallelism is used only where items are independent.** The order of results is fixed by collecting them in input order. Writing files comes first, because it is I/O bound and `GeneratedOutputUniquenessValidator` has already guaranteed that no two outputs share a path. Rendering runs in parallel in slice 0, so that the [concurrency rule for plugin services](#plugin-api) is tested from the start. Compiling follows only if a benchmark on a large model shows a gain. There is no `Flow`: the pipeline is a batch transform, not a stream.
7. **`FileSystem` stays the blocking `java.nio` API.** Host code that does file I/O wraps it on the I/O dispatcher.

#### Coroutines in 4.0

```mermaid
flowchart TB
    subgraph Roots["Composition roots: one runBlocking each"]
        R1["Cli.main"]
        R2["MicrosmithGenerateMojo.execute"]
        R3["MicrosmithGradleWorkerMain"]
        R4["ProcessIsolatedScriptMain"]
    end
    R1 --> Host["MicrosmithScriptHost.run<br/>suspend"]
    R2 --> Host
    R3 --> Host
    R4 --> Exec["ScriptRunExecutor<br/>suspend"]
    Host --> Exec
    Exec --> Eval["ScriptEvaluator on a dedicated thread<br/>that ends with the script run<br/>host.eval blocks"]
    Eval --> Body["Script body<br/>calls emit or generate"]
    Body --> Bridge["The one bridge<br/>runBlocking as a child of the script run Job"]
    Bridge --> Gen["generate<br/>suspend"]
    Gen --> St["Resolve, contribute, assemble, compile, render<br/>sequential, cancellation checked between units"]
    St --> Wr["Write files<br/>parallel on the I/O dispatcher<br/>results collected in input order"]
    Exec --> Proc["ProcessLauncher<br/>suspend, cancelling destroys the process"]
```

#### What this costs

- **Thread-safety obligations.** The host runs file writes in parallel and Maven runs generations in parallel. Plugin services may also be called concurrently within a generation run, so plugin authors take on the [concurrency rule](#plugin-api) too.
- **Tests.** Tests of `suspend` code need `runTest` with a test dispatcher bound in place of the host's dispatcher.
- **The class loader.** Today `defaultPluginClassLoader()` reads the thread context class loader, which belongs to one thread. Work that moves to another dispatcher can see a different one. The generation run takes the plugin class loader explicitly, as [Host composition](#host-composition) says.

### Lifecycle and disposal

A scope says how long an object lives. It does not say how its resources are released. So a generation run has an explicit lifecycle.

```mermaid
sequenceDiagram
    participant Script as Script run
    participant Run as Generation run
    participant A as PluginServices of plugin A
    participant B as PluginServices of plugin B
    participant Pipe as Pipeline
    Script->>Run: create RunGraph
    Note over Run: discover plugins, sort by id
    Run->>A: services(host)
    Run->>B: services(host)
    Run->>Pipe: run
    Pipe-->>Run: result, failure or cancellation
    Note over Run: finally, in a non-cancellable section
    Run->>B: close
    Run->>A: close
    Note over Run: release host resources of the generation run,<br/>such as TemporaryDirectory
    Run-->>Script: result
    Note over Script: after all generation runs have closed,<br/>close the plugin class loader and<br/>interrupt threads that descend from it
```

`PluginServices` extends `AutoCloseable` with a default `close()` that does nothing. A plugin that owns a resource overrides it (illustrative):

```kotlin
internal class IndexedPluginServices(private val index: OpenIndex) : PluginServices {
    override val artifactCompilers: Set<ArtifactCompiler<*>> = setOf(IndexedCompiler(index))

    override fun close() = index.close()
}
```

**Guarantees.**

| Guarantee | Detail |
|---|---|
| `close` always runs | It runs in a `finally`, also when the pipeline throws or is cancelled, in a non-cancellable section. |
| Reverse order | Each plugin's `PluginServices` closes in the reverse of the order that `services(host)` ran. |
| A failing `close` does not hide the original failure | It is reported as a warning diagnostic that names the plugin. When the generation run already failed, it is also added as a suppressed exception to that failure. |
| A failing `close` does not stop the others | The remaining plugins still close. |
| Class loader last | The script run closes the plugin class loader after all its generation runs have closed. |

**What the script run does when it closes the loader.** The script body ran on a dedicated thread that ended with the script run, so a thread local that a plugin set on it is already gone. The script run then closes the plugin class loader and interrupts any thread whose context class loader descends from it. It reports a thread that does not stop as a warning diagnostic that names the plugin class path.

The [collectability spike](#spike-class-loader-collectability) found that the host pins nothing after this. The plugin class loader, its classes and its instances were collected in every case. What keeps the loader reachable is the plugin's own behaviour.

**Guidance for plugin authors.** Plugin services should be stateless transforms, and they must be thread-safe (see the [concurrency rule](#plugin-api)). They should not hold executors, watchers or connections. Nothing a plugin creates may outlive its generation run. Three things are explicitly forbidden:

- A plugin must not leave a thread running.
- A plugin must not leave a thread local set.
- A plugin must not register a shutdown hook.

Each one keeps the plugin class loader reachable from a garbage-collection root. The host can undo only the second, by ending the thread that ran the script. It can stop a plugin thread only if the thread responds to interruption. It cannot remove a shutdown hook without reflection into `java.lang`.

**The app graph has nothing to close.** It holds no resources, by the rule in [Lifetimes](#lifetimes).

## Alternatives considered

| Option | For | Against | Verdict |
|---|---|---|---|
| **Koin** | Its runtime modules are the closest match to ASP.NET's `IServiceCollection`. A runtime container can take contributions from jars loaded later. | Koin types would enter the public plugin API. Missing bindings surface at run time. Koin Annotations is moving to a compiler plugin, which adds the same Kotlin-version coupling as Metro. | Rejected |
| **kotlin-inject** | Checks graphs at compile time. | No runtime aggregation either, so the plugin boundary stays the same. Adds a KSP step to every module. Metro covers the same ground and is already in the build. | Rejected |
| **Custom container** | A runtime registry keyed by type would let plugins contribute to the host. | Gives up build-time checking in every module. Microsmith would have to maintain it. | Rejected |
| **Inject everything** | One uniform mechanism. | Extensions, private helpers and value construction would go through the graph. More wiring with no easier testing, because pure functions are already testable directly. | Rejected |
| **Status quo** | Works today. | Keeps public no-argument constructors as an API requirement, against #203. Keeps I/O untestable outside `cli`. Leaves inline construction in place. | Rejected |

## Trust model

Plugins are trusted code. They run in the host JVM with the host's privileges.

`HostServices` is a testability seam. It is not a capability boundary, and it is not a sandbox. A plugin can call `java.io` directly and bypass every seam.

The controls are on what gets loaded.

```mermaid
flowchart LR
    Req["Plugin requested"] --> Allow["Repository allowlist<br/>RepositoryAllowlistPolicy"]
    Allow --> Sum["Checksum against the lockfile<br/>PluginResolutionIntegrityVerifier"]
    Sum --> Load["Loaded into the plugin class loader<br/>runs with host privileges"]
```

| Control | Class | What it enforces |
|---|---|---|
| Repository allowlist | `RepositoryAllowlistPolicy` | Third-party plugin jars reach the classpath through `PluginResolutionService`. A repository must pass the allowlist: Maven Central, plus `MICROSMITH_REPOSITORY_ALLOWLIST`. `file://` repositories are allowed only with `MICROSMITH_ALLOW_FILE_REPOSITORIES=true`. |
| Checksum verification | `PluginResolutionIntegrityVerifier` | Checks each resolved jar's checksum against the lockfile. |

A real sandbox would need process isolation and an RPC boundary. That is out of scope, and it is a non-goal.

## Consequences and risks

| Risk | Detail |
|---|---|
| Compiler-plugin coupling | Metro is a Kotlin compiler plugin, and each Metro release supports a narrow range of Kotlin versions. Today only `gen` is coupled. After this change, every module that uses Metro, and every third-party plugin that chooses to, must keep its own Kotlin and Metro versions matched. A relocated plugin need not match the host's versions. A hand-wired plugin has no such coupling. |
| Metro runtime across the plugin boundary | An unrelocated third-party Metro plugin can break when a microsmith release bumps Metro, with no change to the plugin API. It may work, but only by luck. The policy is that a third-party Metro plugin relocates its Metro runtime, and an unrelocated one is unsupported. A relocated or hand-wired plugin has no such coupling. The cost falls on plugin authors, who add a shading step to their build. See [Metro across the plugin boundary](#metro-across-the-plugin-boundary). |
| Plugin leaks keep the class loader reachable | A plugin that leaves a thread running, leaves a thread local set or registers a shutdown hook keeps its class loader reachable in a long-lived JVM. The host ends the script thread and interrupts plugin threads, but cannot remove a shutdown hook. The plugin rule forbids all three. See [Lifecycle and disposal](#lifecycle-and-disposal). |
| Every third-party plugin must migrate | The seven `META-INF/services` registrations become one `MicrosmithPlugin`, plus a `MicrosmithScriptingPlugin` if the plugin adds script symbols, and every plugin gains an `id`. This is a 4.0 breaking change. The release notes need a migration section, and [Plugin development](../plugin-development.md) needs a rewrite. |
| Two kinds of checking | Metro checks a plugin's graph when the plugin compiles. The boundary between host and plugin is checked at run time: a missing or failing `MicrosmithPlugin` shows up only when a generation run discovers it. The boundary is deliberately small: two entry interfaces that each carry an `id` and one member that hands over plugin content, plus `HostServices` and `PluginServices`. |
| Concurrency obligations | Parallel Maven builds and parallel file writes need thread-safe seams. Every plugin author also takes on the obligation that plugin services are stateless or thread-safe, because the host calls them concurrently within a generation run. |
| Size | About 724 main files across 38 published modules, plus #203's 1,391 explicit-API warnings. Splitting by module family keeps each pull request reviewable. |
| Host API becomes `suspend` | The script host, the CLI, the Gradle worker and the Maven mojo all change signature. Slices 4 and 5 carry this. |

## Migration plan

Each slice decides injection and visibility for its modules together.

```mermaid
flowchart TB
    S0["0. Foundation<br/>plugin API, AppGraph, RunGraph,<br/>layer rule, engine modules,<br/>acceptance tests"]
    S1["1. Schemas:<br/>protobuf"]
    S2["2. Services:<br/>base and .NET"]
    S3["3. Services:<br/>ASP.NET"]
    S4["4. Script host<br/>script run, scripting contract"]
    S5["5. Integrations<br/>remove old ServiceLoader contracts"]
    Ship(["4.0 ships"])
    S0 --> S1 --> S2 --> S3 --> S4 --> S5 --> Ship
```

| # | Slice | Modules | Main files | #203 warnings | Direct-I/O files |
|---|---|---|---|---|---|
| 0 | Foundation | Plugin API module (`MicrosmithPlugin`, `HostServices`, `PluginServices`, seam interfaces); `AppGraph` and `RunGraph` in `gen`; replacement layer rule and `Path.of` check; `dsl`, `resolve`, `artifact`, `compile`, `gen` | 65 | 127+ (`dsl` 34, `artifact` 37, `gen` 34, `resolve` 22; `compile` not reported separately) | 6, in `gen` |
| 1 | Schemas: protobuf | `schemas/base`, `schemas/protobuf`, `schemas/protobuf-rpc` | 175 | `dsl-schemas-protobuf` 213, `resolve-schemas-protobuf` 145 | few |
| 2 | Services: base and .NET | `services/base`, `services/dotnet`, `services/dotnet-packages` | 120 | `dsl-services-dotnet` 226, `compile-services-dotnet` 158, `dsl-services` 58 | few |
| 3 | Services: ASP.NET | `services/dotnet-asp` | 119 | `dsl-services-dotnet-asp` 80, `resolve-services-dotnet-asp` 73 | few |
| 4 | Script host | `runtime-scripting`, `runtime-scripting-api` | 62 | 44 | 11 |
| 5 | Integrations | `cli`, `gradle-plugin`, `maven-plugin` | 183 | 23 | 47 |

What each slice adds beyond injection and visibility:

| # | Added work |
|---|---|
| 0 | Plugin `id`, sort by id, duplicate and conflict errors that name both plugin ids, and the failure rules. `PluginServices` as `AutoCloseable`, and the generation-run lifecycle. The per-plugin `HostServices` view with its tagged `DiagnosticSink`. The file-system-independent path type and the moves of `ArtifactRenderer` and `GeneratedFile`. `kotlin.time.Clock`. The `suspend` `ProcessLauncher` interface and the dispatcher as a host binding. The `suspend` generation API with parallel file writing, and parallel rendering, which exercises the plugin concurrency rule. The [acceptance tests](#acceptance-tests-for-slice-0). |
| 4 | `MicrosmithScriptingPlugin` and the script run graph. The `ScriptingHostConfiguration` key for the symbol registry. The `suspend` `MicrosmithScriptHost.run` chain, the cancellable bridge and `TimeSource` in `ScriptEvaluator`. The script body on a dedicated thread that ends with the script run. After the script run, interrupting any thread whose context class loader descends from the plugin class loader, and a warning diagnostic that names the plugin class path for one that does not stop. Land #204 before this slice. |
| 5 | One `runBlocking` in each composition root. `ProcessLauncher` implementations that cancel and destroy processes. Thread-safe `Console` and diagnostic emitters. The rewrite of `docs/plugin-development.md`, which documents how a Metro plugin relocates its runtime. Land #205 with this slice. |

- Until slice 5, the host discovers both the old `ServiceLoader` contracts and `MicrosmithPlugin`, so built-in families can move one at a time. Slice 5 removes the old contracts before 4.0 ships.
- Slices 1 to 3 are mostly about objects, inline construction and visibility. The I/O work is in slices 4 and 5.
- #204 may change the `ScriptSymbolContributor` contract. #205 settles the `timestamp` format that the injected clock produces. Landing them in this order means each contract changes only once.
- Each slice updates the docs it makes untrue: `docs/architecture-principles.md` ("Metro is internal", "ServiceLoader boundary"), `docs/contributing.md` (module layering), `docs/plugin-development.md` and `AGENTS.md`.

## Validation

Three spikes back this design. The first tested the graph boundary across separately compiled jars. The second tested a plugin with a relocated Metro runtime. The third tested whether a plugin class loader is collectable after a script run. Slice 0 then adds acceptance tests.

### Spike results

Setup: Kotlin 2.4.21, Metro 1.5.2, Gradle 9.9.1, JDK 25.

```mermaid
flowchart LR
    subgraph ApiM["api: no Metro"]
        HSI["HostServices"]
        SM["Scope markers"]
    end
    subgraph PluginM["plugin: Metro"]
        DG["DemoGraph and its generated Impl"]
    end
    subgraph HostM["host: Metro"]
        AG["AppGraph"] --> RG["RunGraph<br/>GraphExtension of RunScope"]
    end
    PluginM -->|"compileOnly"| ApiM
    HostM -->|"implementation"| ApiM
    HostM -.->|"URLClassLoader and ServiceLoader,<br/>at run time only"| PluginM
```

The plugin jar held only plugin classes and Metro's generated `DemoGraph$Impl` and factories. The host loaded it through a `URLClassLoader` whose parent was the host loader. The Metro runtime came from the host classpath with no extra configuration.

| Case | Result |
|---|---|
| Plugin class and jar absent from the host's compile classpath | Worked |
| Host services passed through `@Includes host: HostServices` reach plugin constructors | Worked: an injected sink received messages |
| `@SingleIn(PluginScope)` resolver | The same instance within a generation run, a new instance across generation runs |
| App singleton | Shared across generation runs |
| `@AssistedInject` with `@AssistedFactory`, mixing a runtime argument and an injected `Clock` | Worked |
| Every Metro API name used in this document | Exists in 1.5.2 |
| **Expected failure:** plugin graph as a `@GraphExtension(RunScope)` of the host, with its factory `@ContributesTo(AppScope)` | Compiles, then fails at run time: `ClassCastException: AppGraph$Impl$RunGraphImpl cannot be cast to PluginRunGraph$Factory`. The host graph is generated when the host compiles and cannot know the plugin. |
| **Expected failure:** plugin `@ContributesIntoSet(RunScope)` into the host graph | Compiles and loads, but the host set is empty at run time. The host also fails to compile with `[Metro/EmptyMultibinding]` until the set is declared `@Multibinds(allowEmpty = true)`. Aggregation happens only when the host compiles. |

The two expected failures confirm why each plugin has a standalone graph.

The spike also showed that scope markers used on both sides of the boundary must live in the shared API module.

### Spike: relocated Metro runtime

Question: does a third-party plugin that relocates its Metro runtime run against a host on another Metro version? Result: yes.

Setup: three separate Gradle builds. An `api` with no Metro. A host on Metro 1.5.2 with Kotlin 2.4.21 and its own graph. A plugin built at five Metro versions. The host loaded each plugin jar through a parent-first `URLClassLoader` and `ServiceLoader`. Each run used a multibound set that takes a host service, a scoped binding and an assisted factory. JDK 25, Gradle 9.9.1, Shadow 9.6.1.

| Plugin compiled with | Unrelocated jar | Relocated jar |
|---|---|---|
| Metro 0.6.10, Kotlin 2.2.21 | Fails: `NoSuchMethodError: InstanceFactory$Companion.invoke(Object)` in the generated graph constructor | Works |
| Metro 1.0.1, Kotlin 2.2.21 | Works, by luck | Works |
| Metro 1.1.0, Kotlin 2.3.21 | Works, by luck | Works |
| Metro 1.4.5, Kotlin 2.4.21 | Works, by luck | Works |
| Metro 1.5.2, Kotlin 2.4.21 (control) | Works | Works |

Findings:

- Class loading traces showed that unrelocated plugins used `dev.zacsweers.metro.*` from the host's runtime jar, and that relocated plugins used their own relocated copy.
- Relocation was complete. No class, method or field reference to the original package remained.
- The Metro runtime depends only on `kotlin-stdlib`. The plugin must not shade the standard library: the host's 2.4.21 served a plugin built with Kotlin 2.2.21.
- The shaded runtime adds about 158 KB for Metro 1.5.2.
- The runtime uses no `Class.forName`, `ServiceLoader` or reflection.

Not tested: a host standard library older than the plugin's Metro runtime needs, jar minimisation, and `kotlin-reflect` on Metro types.

The decision is in [Metro across the plugin boundary](#metro-across-the-plugin-boundary), which also shows the build configuration.

### Spike: class loader collectability

Question: does anything in the host keep a plugin class loader reachable after a script run? Result: no. The loader is already collectable.

Setup: an integration test ran scripts through the script host in class loader isolation mode. The plugin jar registered a `ScriptSymbolContributor`, a `DomainResolver` and an `ArtifactContributor`, and the script used all three. The test held only weak and phantom references.

The plugin `URLClassLoader`, a plugin class and plugin instances were collected in every case:

- one run
- run A then run B, with different plugin class paths
- the same jar twice, as a compile cache hit
- a compile failure
- an evaluation failure
- twelve alternating runs in one JVM
- G1, Serial, Parallel, ZGC and Shenandoah

Run B never saw a class or instance from run A. The thread context class loader is restored after each run.

What does keep the loader reachable is the plugin's own behaviour:

| Leak by the plugin | GC root | Can the host release it? |
|---|---|---|
| A thread local set on the calling thread | The thread's `threadLocals` map | Yes, by running the script on a thread that ends with the script run. |
| A thread the plugin starts | The thread. Its context class loader is the compiled-script loader, whose parent is the plugin loader. | Only if the thread responds to interruption. |
| A shutdown hook | `ApplicationShutdownHooks` | Not without reflection into `java.lang`. |

Limits: the plugin was a small Java jar. It was not resolved from Maven and did not use Metro. Process isolation was not tested, because it ends the JVM.

The test shape that proved reliable: capture weak references inside the run, drop every strong reference, and call `System.gc()` in a bounded loop until the reference clears. It keeps one negative-control case, a thread-local leak on the calling thread, so that the test is shown to be able to fail. It cannot run under `-XX:+DisableExplicitGC`.

The consequences are in [Lifecycle and disposal](#lifecycle-and-disposal) and in [the coroutine decision](#decision-option-b).

### Acceptance tests for slice 0

Slice 0 automates these scenarios as integration tests. Each one composes several stages with real collaborators and loads built plugin jars through a class loader, as `AGENTS.md` asks of the integration layer.

| Scenario | What it protects |
|---|---|
| Two separately compiled plugins loaded together | The plugin boundary works across jars with no shared compile classpath. |
| Two plugins with a conflicting contribution | The conflict rules in [Conflicts](#conflicts): merge identical content, fail on differing content, and name both plugin ids. |
| Duplicate plugin id | The generation run fails before the pipeline starts, and the error names the id. |
| A plugin whose `services` throws | One failing plugin fails the whole generation run, with a diagnostic that names it. No partial loading. |
| Two concurrent `generate` calls in one JVM | App-scoped seams are thread-safe. Diagnostics and progress do not leak between runs. |
| Repeated script runs with different plugin class paths | Run B never sees a class or instance from run A, and the thread context class loader is restored after each run. |
| Plugin class loader collectability | The plugin class loader, a plugin class and a plugin instance are collected after a script run. The test captures weak references inside the run, drops every strong reference, and calls `System.gc()` in a bounded loop until each one clears. A negative control must keep the loader reachable. It is a plugin that leaves a thread running that ignores interruption, because a thread local no longer pins the loader once the script runs on its own thread. The host reports a warning that names the plugin class path. The test cannot run under `-XX:+DisableExplicitGC`. |
| The pipeline fails midway | Every plugin's `PluginServices` is still closed, in reverse order. |
| Shuffled discovery order | Output is byte-identical whatever the `ServiceLoader` order. |
| Parallel rendering with a plugin that holds no state | The host calls one plugin's services concurrently within a generation run, and the output is byte-identical to a sequential run. |
| A Metro-using plugin built against another Metro version | Follows the [Metro policy](#metro-across-the-plugin-boundary). A relocated plugin works. An unrelocated plugin built against an incompatible version is reported as an incompatible plugin. |

All of these are ordinary tests, written with the foundation slice. Nothing needs a spike first. The [relocation spike](#spike-relocated-metro-runtime) and the [collectability spike](#spike-class-loader-collectability) set the expected results of the Metro row and the class loader row.

## Settled questions

The fourteen questions that were open during review now have outcomes. The numbers are kept so that earlier discussion still maps.

| # | Question | Outcome | Where |
|---|---|---|---|
| 1 | Where script symbol contributors live, since they are needed outside a generation run | A separate, optional contract, `MicrosmithScriptingPlugin`, in `runtime-scripting-api`. It is not a method on `MicrosmithPlugin`, and `scriptSymbolContributors` leaves `PluginServices`. | [Plugin API](#plugin-api) |
| 2 | Script entry points that the scripting framework instantiates | The host puts the symbol registry in a `ScriptingHostConfiguration` key. Refiners fall back to discovery when the key is absent, as in the IDE. Script-definition objects join Mappie mappers as exceptions to Rule 1. | [Exceptions to the rules](#exceptions-to-the-rules) |
| 3 | Paths in pipeline models | A file-system-independent relative-path value type in `artifact`, covering ids, output roots and `GeneratedFile`. | [Paths in pipeline models](#paths-in-pipeline-models) |
| 4 | DSL entry points as extensions | An explicit exception to the extension rule. | [Exceptions to the rules](#exceptions-to-the-rules) |
| 5 | Where the plugin API lives | `ArtifactRenderer` and `GeneratedFile` move down. Plugins do not compile against `gen`. | [Where the plugin API lives](#where-the-plugin-api-lives) |
| 6 | What counts as one built-in plugin | One plugin per module family. | [Built-in plugins](#built-in-plugins) |
| 7 | The script compile cache is not immutable | It is script-run state, built once per script run. The app singleton keeps only the cache directory and fingerprint policy. | [Lifetimes](#lifetimes) |
| 8 | Coroutine dispatcher | A host binding, not exposed to plugins. | [I/O seams](#io-seams), [Coroutines and `suspend`](#coroutines-and-suspend) |
| 9 | Clock type | `kotlin.time.Clock`, which is stable since Kotlin 2.3. A separate `TimeSource` measures durations. | [Time: two seams](#time-two-seams) |
| 10 | Overlap with #204 and #205 | Land #204 before slice 4, and #205 with slice 5. | [Migration plan](#migration-plan) |
| 11 | Coroutine direction | Structured concurrency in the host. The plugin API stays non-`suspend`. Option a kept today's gaps, and option c would colour every plugin for no gain. | [Coroutines and `suspend`](#coroutines-and-suspend) |
| 12 | Concurrent calls into one plugin's services | The host may call them concurrently within a generation run. Plugin services must be stateless or thread-safe. Parallel rendering in slice 0 exercises it. | [Plugin API](#plugin-api) |
| 13 | A plugin descriptor beyond `id` | `id` only. Compatibility follows the semver of the plugin API module, and a `LinkageError` at the boundary is reported as an incompatible plugin. | [Identity](#identity) |
| 14 | Metro policy for third-party plugins | A third-party plugin that uses Metro relocates its Metro runtime. An unrelocated one is unsupported: it may work, but only by luck, and the host reports an incompatible plugin when it fails. Hand-wiring remains a fully supported alternative. Built-in plugins share the host's runtime. | [Metro across the plugin boundary](#metro-across-the-plugin-boundary) |

[Back to the README](../../README.md)
