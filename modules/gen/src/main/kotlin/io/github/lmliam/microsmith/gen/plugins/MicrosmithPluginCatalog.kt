package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.gen.ArtifactRenderer
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionIssueDiagnosticMapper
import java.util.ServiceLoader
import kotlin.reflect.KClass

class MicrosmithPluginCatalog(
    domainResolvers: Iterable<DomainResolver<*, *>> = emptyList(),
    artifactContributors: Iterable<ArtifactContributor<*>> = emptyList(),
    artifactAssemblers: Iterable<ArtifactAssembler<*>> = emptyList(),
    artifactCompilers: Iterable<ArtifactCompiler<*>> = emptyList(),
    artifactRenderers: Iterable<ArtifactRenderer<*>> = emptyList(),
    resolutionIssueDiagnosticMappers: Iterable<ResolutionIssueDiagnosticMapper<*>> = emptyList(),
) {
    val domainResolvers: List<DomainResolver<*, *>> = domainResolvers.toList()
    val artifactContributors: List<ArtifactContributor<*>> = artifactContributors.toList()
    val artifactAssemblers: List<ArtifactAssembler<*>> = artifactAssemblers.toList()
    val artifactCompilers: List<ArtifactCompiler<*>> = artifactCompilers.toList()
    val artifactRenderers: List<ArtifactRenderer<*>> = artifactRenderers.toList()
    val resolutionIssueDiagnosticMappers: List<ResolutionIssueDiagnosticMapper<*>> =
        resolutionIssueDiagnosticMappers.toList()
}

fun discoverMicrosmithPlugins(classLoader: ClassLoader = defaultPluginClassLoader()): MicrosmithPluginCatalog =
    MicrosmithPluginCatalog(
        domainResolvers = loadServices(DomainResolver::class, classLoader),
        artifactContributors = loadServices(ArtifactContributor::class, classLoader),
        artifactAssemblers = loadServices(ArtifactAssembler::class, classLoader),
        artifactCompilers = loadServices(ArtifactCompiler::class, classLoader),
        artifactRenderers = loadServices(ArtifactRenderer::class, classLoader),
        resolutionIssueDiagnosticMappers = loadServices(ResolutionIssueDiagnosticMapper::class, classLoader),
    )

private fun defaultPluginClassLoader(): ClassLoader =
    Thread.currentThread().contextClassLoader ?: MicrosmithPluginCatalog::class.java.classLoader

private fun <T : Any> loadServices(type: KClass<T>, classLoader: ClassLoader): List<T> = ServiceLoader
    .load(type.java, classLoader)
    .iterator()
    .asSequence()
    .toList()
