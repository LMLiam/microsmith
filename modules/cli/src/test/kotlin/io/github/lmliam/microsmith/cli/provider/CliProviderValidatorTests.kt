package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileContribution
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

class CliProviderValidatorTests :
    StringSpec({
        "reports missing built-in providers when none are present" {
            val errors =
                verifyBuiltinProviders(
                    domainResolvers = emptyList(),
                    artifactContributors = emptyList(),
                    artifactAssemblers = emptyList(),
                    artifactRenderers = emptyList(),
                )

            errors.shouldHaveSize(6)
            errors.shouldContain(
                "Missing built-in DomainResolver for SchemasExtension -> " +
                    "ResolvedProtobufSchemaModel. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in DomainResolver for SchemasExtension -> " +
                    "ResolvedProtobufRpcSchemaModel. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactContributor for ResolvedProtobufSchemaModel. " +
                    "Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactContributor for ResolvedProtobufRpcSchemaModel. " +
                    "Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for ProtoFileArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactRenderer for ProtoFileArtifact. Check CLI runtime packaging.",
            )
        }

        "returns no errors when required providers are present" {
            val errors =
                verifyBuiltinProviders(
                    domainResolvers = listOf(ProtobufResolverStub(), ProtobufRpcResolverStub()),
                    artifactContributors = listOf(ProtobufContributorStub(), ProtobufRpcContributorStub()),
                    artifactAssemblers = listOf(ProtoFileAssemblerStub()),
                    artifactRenderers = listOf(ProtoFileRendererStub()),
                )

            errors shouldBe emptyList()
        }
    })

private class ProtobufResolverStub : DomainResolver<SchemasExtension, ResolvedProtobufSchemaModel> {
    override val authoringType: KClass<SchemasExtension> = SchemasExtension::class
    override val resolvedType: KClass<ResolvedProtobufSchemaModel> = ResolvedProtobufSchemaModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedProtobufSchemaModel =
        ResolvedProtobufSchemaModel(emptyList())
}

private class ProtobufRpcResolverStub : DomainResolver<SchemasExtension, ResolvedProtobufRpcSchemaModel> {
    override val authoringType: KClass<SchemasExtension> = SchemasExtension::class
    override val resolvedType: KClass<ResolvedProtobufRpcSchemaModel> = ResolvedProtobufRpcSchemaModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedProtobufRpcSchemaModel =
        ResolvedProtobufRpcSchemaModel(emptyList())
}

private class ProtobufContributorStub : ArtifactContributor<ResolvedProtobufSchemaModel> {
    override val resolvedType: KClass<ResolvedProtobufSchemaModel> = ResolvedProtobufSchemaModel::class

    override fun contribute(model: ResolvedProtobufSchemaModel): List<ArtifactContribution<*>> = emptyList()
}

private class ProtobufRpcContributorStub : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
    override val resolvedType: KClass<ResolvedProtobufRpcSchemaModel> = ResolvedProtobufRpcSchemaModel::class

    override fun contribute(model: ResolvedProtobufRpcSchemaModel): List<ArtifactContribution<*>> = emptyList()
}

private class ProtoFileAssemblerStub : ArtifactAssembler<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    override fun create(first: ArtifactContribution<ProtoFileArtifact>): ProtoFileArtifact {
        val contribution = first as ProtoFileContribution
        return ProtoFileArtifact(
            id = contribution.artifactId,
            packageName = contribution.packageName,
            imports = contribution.imports,
            declarations = contribution.declarations,
        )
    }

    override fun merge(
        current: ProtoFileArtifact,
        contribution: ArtifactContribution<ProtoFileArtifact>,
    ): ProtoFileArtifact = current
}

private class ProtoFileRendererStub : ArtifactRenderer<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    override fun render(artifact: ProtoFileArtifact): GeneratedFile = GeneratedFile(
        relativePath = artifact.id.relativePath,
        contents = artifact.declarations.joinToString("\n") { it.contents }.toByteArray(),
        outputRoot = artifact.id.outputRoot,
    )
}
