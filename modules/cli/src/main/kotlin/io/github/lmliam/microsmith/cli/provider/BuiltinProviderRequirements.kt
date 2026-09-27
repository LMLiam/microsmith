package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution.DotnetPackageWorkspace
import kotlin.reflect.KClass

internal fun missingBuiltinProviderMessages(pluginCatalog: MicrosmithPluginCatalog): List<String> =
    builtinProviderRequirements.mapNotNull { requirement ->
        requirement.missingMessage(pluginCatalog)
    }

private val builtinProviderRequirements =
    listOf(
        domainResolverRequirement(SchemasExtension::class, ResolvedProtobufSchemaModel::class),
        domainResolverRequirement(SchemasExtension::class, ResolvedProtobufRpcSchemaModel::class),
        domainResolverRequirement(ServicesExtension::class, DotnetAspWorkspace::class),
        domainResolverRequirement(ServicesExtension::class, DotnetPackageWorkspace::class),
        artifactContributorRequirement(ResolvedProtobufSchemaModel::class),
        artifactContributorRequirement(ResolvedProtobufRpcSchemaModel::class),
        artifactContributorRequirement(DotnetAspWorkspace::class),
        artifactContributorRequirement(DotnetPackageWorkspace::class),
        artifactAssemblerRequirement(ProtoFileArtifact::class),
        artifactAssemblerRequirement(ProtobufRpcServiceArtifact::class),
        artifactAssemblerRequirement(DotnetAspServiceArtifact::class),
        artifactAssemblerRequirement(DotnetPackageVersionsArtifact::class),
        artifactAssemblerRequirement(DotnetPackageReferencesArtifact::class),
        artifactAssemblerRequirement(MsBuildProjectArtifact::class),
        artifactAssemblerRequirement(TextFileArtifact::class),
        artifactCompilerRequirement(ProtoFileArtifact::class),
        artifactCompilerRequirement(ProtobufRpcServiceArtifact::class),
        artifactCompilerRequirement(DotnetAspServiceArtifact::class),
        artifactCompilerRequirement(DotnetPackageVersionsArtifact::class),
        artifactCompilerRequirement(DotnetPackageReferencesArtifact::class),
        artifactCompilerRequirement(MsBuildProjectArtifact::class),
        artifactRendererRequirement(TextFileArtifact::class),
    )

private data class BuiltinProviderRequirement(
    val description: String,
    val isPresent: (MicrosmithPluginCatalog) -> Boolean,
) {
    fun missingMessage(pluginCatalog: MicrosmithPluginCatalog): String? = if (isPresent(pluginCatalog)) {
        null
    } else {
        "Missing built-in $description. Check CLI runtime packaging."
    }
}

private fun domainResolverRequirement(authoringType: KClass<*>, resolvedType: KClass<*>): BuiltinProviderRequirement =
    BuiltinProviderRequirement(
        description = "DomainResolver for ${authoringType.displayName()} -> ${resolvedType.displayName()}",
        isPresent = { pluginCatalog ->
            pluginCatalog.domainResolvers.any { resolver ->
                resolver.authoringType == authoringType &&
                    resolver.resolvedType == resolvedType
            }
        },
    )

private fun artifactContributorRequirement(resolvedType: KClass<*>): BuiltinProviderRequirement =
    BuiltinProviderRequirement(
        description = "ArtifactContributor for ${resolvedType.displayName()}",
        isPresent = { pluginCatalog ->
            pluginCatalog.artifactContributors.any { contributor ->
                contributor.resolvedType == resolvedType
            }
        },
    )

private fun artifactAssemblerRequirement(artifactType: KClass<*>): BuiltinProviderRequirement =
    BuiltinProviderRequirement(
        description = "ArtifactAssembler for ${artifactType.displayName()}",
        isPresent = { pluginCatalog ->
            pluginCatalog.artifactAssemblers.any { assembler ->
                assembler.artifactType == artifactType
            }
        },
    )

private fun artifactCompilerRequirement(artifactType: KClass<*>): BuiltinProviderRequirement =
    BuiltinProviderRequirement(
        description = "ArtifactCompiler for ${artifactType.displayName()}",
        isPresent = { pluginCatalog ->
            pluginCatalog.artifactCompilers.any { compiler ->
                compiler.artifactType == artifactType
            }
        },
    )

private fun artifactRendererRequirement(artifactType: KClass<*>): BuiltinProviderRequirement =
    BuiltinProviderRequirement(
        description = "ArtifactRenderer for ${artifactType.displayName()}",
        isPresent = { pluginCatalog ->
            pluginCatalog.artifactRenderers.any { renderer ->
                renderer.artifactType == artifactType
            }
        },
    )

private fun KClass<*>.displayName(): String = simpleName ?: qualifiedName ?: toString()
