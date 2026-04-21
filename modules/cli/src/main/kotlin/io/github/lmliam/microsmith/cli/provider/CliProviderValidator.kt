package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackageWorkspace
import java.util.ServiceLoader

internal fun verifyBuiltinProviders(
    domainResolvers: List<DomainResolver<*, *>> = loadDomainResolvers(),
    artifactContributors: List<ArtifactContributor<*>> = loadArtifactContributors(),
    artifactAssemblers: List<ArtifactAssembler<*>> = loadArtifactAssemblers(),
    artifactCompilers: List<ArtifactCompiler<*>> = loadArtifactCompilers(),
    artifactRenderers: List<ArtifactRenderer<*>> = loadArtifactRenderers(),
): List<String> {
    val errors = mutableListOf<String>()

    errors += requireDomainResolver(domainResolvers, SchemasExtension::class, ResolvedProtobufSchemaModel::class)
    errors += requireDomainResolver(domainResolvers, SchemasExtension::class, ResolvedProtobufRpcSchemaModel::class)
    errors += requireDomainResolver(domainResolvers, ServicesExtension::class, DotnetAspWorkspace::class)
    errors += requireDomainResolver(domainResolvers, ServicesExtension::class, DotnetPackageWorkspace::class)

    errors += requireArtifactContributor(artifactContributors, ResolvedProtobufSchemaModel::class)
    errors += requireArtifactContributor(artifactContributors, ResolvedProtobufRpcSchemaModel::class)
    errors += requireArtifactContributor(artifactContributors, DotnetAspWorkspace::class)
    errors += requireArtifactContributor(artifactContributors, DotnetPackageWorkspace::class)

    errors += requireArtifactAssembler(artifactAssemblers, ProtoFileArtifact::class)
    errors += requireArtifactAssembler(artifactAssemblers, ProtobufRpcServiceArtifact::class)
    errors += requireArtifactAssembler(artifactAssemblers, DotnetAspServiceArtifact::class)
    errors += requireArtifactAssembler(artifactAssemblers, DotnetPackageVersionsArtifact::class)
    errors += requireArtifactAssembler(artifactAssemblers, DotnetPackageReferencesArtifact::class)
    errors += requireArtifactAssembler(artifactAssemblers, MsBuildProjectArtifact::class)
    errors += requireArtifactAssembler(artifactAssemblers, TextFileArtifact::class)

    errors += requireArtifactCompiler(artifactCompilers, ProtoFileArtifact::class)
    errors += requireArtifactCompiler(artifactCompilers, ProtobufRpcServiceArtifact::class)
    errors += requireArtifactCompiler(artifactCompilers, DotnetAspServiceArtifact::class)
    errors += requireArtifactCompiler(artifactCompilers, DotnetPackageVersionsArtifact::class)
    errors += requireArtifactCompiler(artifactCompilers, DotnetPackageReferencesArtifact::class)
    errors += requireArtifactCompiler(artifactCompilers, MsBuildProjectArtifact::class)

    errors += requireArtifactRenderer(artifactRenderers, TextFileArtifact::class)

    return errors
}

private fun requireDomainResolver(
    domainResolvers: List<DomainResolver<*, *>>,
    authoringType: kotlin.reflect.KClass<*>,
    resolvedType: kotlin.reflect.KClass<*>,
): List<String> = if (
    domainResolvers.none {
        it.authoringType == authoringType && it.resolvedType == resolvedType
    }
) {
    listOf(
        "Missing built-in DomainResolver for ${authoringType.simpleName} -> " +
            "${resolvedType.simpleName}. Check CLI runtime packaging.",
    )
} else {
    emptyList()
}

private fun requireArtifactContributor(
    artifactContributors: List<ArtifactContributor<*>>,
    resolvedType: kotlin.reflect.KClass<*>,
): List<String> = if (artifactContributors.none { it.resolvedType == resolvedType }) {
    listOf("Missing built-in ArtifactContributor for ${resolvedType.simpleName}. Check CLI runtime packaging.")
} else {
    emptyList()
}

private fun requireArtifactAssembler(
    artifactAssemblers: List<ArtifactAssembler<*>>,
    artifactType: kotlin.reflect.KClass<*>,
): List<String> = if (artifactAssemblers.none { it.artifactType == artifactType }) {
    listOf("Missing built-in ArtifactAssembler for ${artifactType.simpleName}. Check CLI runtime packaging.")
} else {
    emptyList()
}

private fun requireArtifactCompiler(
    artifactCompilers: List<ArtifactCompiler<*>>,
    artifactType: kotlin.reflect.KClass<*>,
): List<String> = if (artifactCompilers.none { it.artifactType == artifactType }) {
    listOf("Missing built-in ArtifactCompiler for ${artifactType.simpleName}. Check CLI runtime packaging.")
} else {
    emptyList()
}

private fun requireArtifactRenderer(
    artifactRenderers: List<ArtifactRenderer<*>>,
    artifactType: kotlin.reflect.KClass<*>,
): List<String> = if (artifactRenderers.none { it.artifactType == artifactType }) {
    listOf("Missing built-in ArtifactRenderer for ${artifactType.simpleName}. Check CLI runtime packaging.")
} else {
    emptyList()
}

private fun loadDomainResolvers() = ServiceLoader.load(DomainResolver::class.java)
    .iterator()
    .asSequence()
    .toList()

private fun loadArtifactContributors() = ServiceLoader.load(ArtifactContributor::class.java)
    .iterator()
    .asSequence()
    .toList()

private fun loadArtifactAssemblers() = ServiceLoader.load(ArtifactAssembler::class.java)
    .iterator()
    .asSequence()
    .toList()

private fun loadArtifactCompilers() = ServiceLoader.load(ArtifactCompiler::class.java)
    .iterator()
    .asSequence()
    .toList()

private fun loadArtifactRenderers() = ServiceLoader.load(ArtifactRenderer::class.java)
    .iterator()
    .asSequence()
    .toList()
