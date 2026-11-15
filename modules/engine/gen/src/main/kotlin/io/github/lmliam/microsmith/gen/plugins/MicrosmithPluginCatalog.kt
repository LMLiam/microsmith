package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.ArtifactRenderer
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import java.util.ServiceLoader
import kotlin.reflect.KClass

@InternalMicrosmithApi
public class MicrosmithPluginCatalog(
    domainResolvers: Iterable<DomainResolver<*, *>> = emptyList(),
    artifactContributors: Iterable<ArtifactContributor<*>> = emptyList(),
    artifactAssemblers: Iterable<ArtifactAssembler<*>> = emptyList(),
    artifactCompilers: Iterable<ArtifactCompiler<*>> = emptyList(),
    artifactRenderers: Iterable<ArtifactRenderer<*>> = emptyList(),
    resolutionIssueDiagnosticMappers: Iterable<ResolutionIssueDiagnosticMapper<*>> = emptyList(),
) {
    public val domainResolvers: List<DomainResolver<*, *>> = domainResolvers.toList()
    public val artifactContributors: List<ArtifactContributor<*>> = artifactContributors.toList()
    public val artifactAssemblers: List<ArtifactAssembler<*>> = artifactAssemblers.toList()
    public val artifactCompilers: List<ArtifactCompiler<*>> = artifactCompilers.toList()
    public val artifactRenderers: List<ArtifactRenderer<*>> = artifactRenderers.toList()
    public val resolutionIssueDiagnosticMappers: List<ResolutionIssueDiagnosticMapper<*>> =
        resolutionIssueDiagnosticMappers.toList()

    internal operator fun plus(other: MicrosmithPluginCatalog): MicrosmithPluginCatalog =
        MicrosmithPluginCatalog(
            domainResolvers = domainResolvers + other.domainResolvers,
            artifactContributors = artifactContributors + other.artifactContributors,
            artifactAssemblers = artifactAssemblers + other.artifactAssemblers,
            artifactCompilers = artifactCompilers + other.artifactCompilers,
            artifactRenderers = artifactRenderers + other.artifactRenderers,
            resolutionIssueDiagnosticMappers =
                resolutionIssueDiagnosticMappers + other.resolutionIssueDiagnosticMappers,
        )
}

@InternalMicrosmithApi
public fun discoverMicrosmithPlugins(classLoader: ClassLoader): MicrosmithPluginCatalog =
    MicrosmithPluginCatalog(
        domainResolvers = loadServices(DomainResolver::class, classLoader),
        artifactContributors = loadServices(ArtifactContributor::class, classLoader),
        artifactAssemblers = loadServices(ArtifactAssembler::class, classLoader),
        artifactCompilers = loadServices(ArtifactCompiler::class, classLoader),
        artifactRenderers = loadServices(ArtifactRenderer::class, classLoader),
        resolutionIssueDiagnosticMappers = loadServices(ResolutionIssueDiagnosticMapper::class, classLoader),
    )

private fun <T : Any> loadServices(type: KClass<T>, classLoader: ClassLoader): List<T> =
    ServiceLoader.load(type.java, classLoader).iterator().asSequence().toList()
