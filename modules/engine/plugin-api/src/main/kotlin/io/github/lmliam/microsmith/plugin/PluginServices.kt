package io.github.lmliam.microsmith.plugin

import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.ArtifactRenderer
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper

/**
 * The services a plugin contributes to one generation run.
 *
 * The host may call these services from several threads at once within the run, so they must be stateless or
 * thread-safe. The host calls [close] when the generation run ends.
 */
public interface PluginServices : AutoCloseable {
    public val domainResolvers: Set<DomainResolver<*, *>>
        get() = emptySet()

    public val resolutionIssueDiagnosticMappers: Set<ResolutionIssueDiagnosticMapper<*>>
        get() = emptySet()

    public val artifactContributors: Set<ArtifactContributor<*>>
        get() = emptySet()

    public val artifactAssemblers: Set<ArtifactAssembler<*>>
        get() = emptySet()

    public val artifactCompilers: Set<ArtifactCompiler<*>>
        get() = emptySet()

    public val artifactRenderers: Set<ArtifactRenderer<*>>
        get() = emptySet()

    public override fun close() {}
}
