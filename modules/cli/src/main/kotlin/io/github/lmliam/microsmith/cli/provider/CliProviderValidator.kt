package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import java.util.ServiceLoader

internal fun verifyBuiltinProviders(
    domainResolvers: List<DomainResolver<*, *>> = loadDomainResolvers(),
    artifactContributors: List<ArtifactContributor<*>> = loadArtifactContributors(),
    artifactAssemblers: List<ArtifactAssembler<*>> = loadArtifactAssemblers(),
    artifactCompilers: List<ArtifactCompiler<*>> = loadArtifactCompilers(),
    artifactRenderers: List<ArtifactRenderer<*>> = loadArtifactRenderers(),
): List<String> {
    val errors = mutableListOf<String>()

    if (
        domainResolvers.none {
            it.authoringType == SchemasExtension::class &&
                it.resolvedType == ResolvedProtobufSchemaModel::class
        }
    ) {
        errors += "Missing built-in DomainResolver for SchemasExtension -> " +
            "ResolvedProtobufSchemaModel. Check CLI runtime packaging."
    }

    if (
        domainResolvers.none {
            it.authoringType == SchemasExtension::class &&
                it.resolvedType == ResolvedProtobufRpcSchemaModel::class
        }
    ) {
        errors += "Missing built-in DomainResolver for SchemasExtension -> " +
            "ResolvedProtobufRpcSchemaModel. Check CLI runtime packaging."
    }

    if (artifactContributors.none { it.resolvedType == ResolvedProtobufSchemaModel::class }) {
        errors += "Missing built-in ArtifactContributor for ResolvedProtobufSchemaModel. " +
            "Check CLI runtime packaging."
    }

    if (artifactContributors.none { it.resolvedType == ResolvedProtobufRpcSchemaModel::class }) {
        errors += "Missing built-in ArtifactContributor for ResolvedProtobufRpcSchemaModel. " +
            "Check CLI runtime packaging."
    }

    if (artifactAssemblers.none { it.artifactType == ProtoFileArtifact::class }) {
        errors += "Missing built-in ArtifactAssembler for ProtoFileArtifact. Check CLI runtime packaging."
    }

    if (artifactAssemblers.none { it.artifactType == ProtobufRpcServiceArtifact::class }) {
        errors += "Missing built-in ArtifactAssembler for ProtobufRpcServiceArtifact. Check CLI runtime packaging."
    }

    if (artifactAssemblers.none { it.artifactType == TextFileArtifact::class }) {
        errors += "Missing built-in ArtifactAssembler for TextFileArtifact. Check CLI runtime packaging."
    }

    if (artifactCompilers.none { it.artifactType == ProtoFileArtifact::class }) {
        errors += "Missing built-in ArtifactCompiler for ProtoFileArtifact. Check CLI runtime packaging."
    }

    if (artifactCompilers.none { it.artifactType == ProtobufRpcServiceArtifact::class }) {
        errors += "Missing built-in ArtifactCompiler for ProtobufRpcServiceArtifact. Check CLI runtime packaging."
    }

    if (artifactRenderers.none { it.artifactType == TextFileArtifact::class }) {
        errors += "Missing built-in ArtifactRenderer for TextFileArtifact. Check CLI runtime packaging."
    }

    return errors
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
