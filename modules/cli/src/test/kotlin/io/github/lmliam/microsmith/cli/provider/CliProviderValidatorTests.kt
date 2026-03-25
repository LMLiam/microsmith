package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.core.ArtifactContributor
import io.github.lmliam.microsmith.artifact.files.TextFileArtifact
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
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
                    artifactCompilers = emptyList(),
                    artifactRenderers = emptyList(),
                )

            errors.shouldHaveSize(10)
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
                "Missing built-in ArtifactAssembler for ProtobufRpcServiceArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for TextFileArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactCompiler for ProtoFileArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactCompiler for ProtobufRpcServiceArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactRenderer for TextFileArtifact. Check CLI runtime packaging.",
            )
        }

        "returns no errors when required providers are present" {
            val errors =
                verifyBuiltinProviders(
                    domainResolvers = listOf(ProtobufResolverStub(), ProtobufRpcResolverStub()),
                    artifactContributors = listOf(ProtobufContributorStub(), ProtobufRpcContributorStub()),
                    artifactAssemblers = listOf(
                        ProtoFileAssemblerStub(),
                        ProtobufRpcAssemblerStub(),
                        TextFileAssemblerStub(),
                    ),
                    artifactCompilers = listOf(ProtoFileCompilerStub(), ProtobufRpcCompilerStub()),
                    artifactRenderers = listOf(TextFileRendererStub()),
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

private class ProtobufRpcAssemblerStub : ArtifactAssembler<ProtobufRpcServiceArtifact> {
    override val artifactType: KClass<ProtobufRpcServiceArtifact> = ProtobufRpcServiceArtifact::class

    override fun create(first: ArtifactContribution<ProtobufRpcServiceArtifact>): ProtobufRpcServiceArtifact {
        error("Not used in provider validator tests.")
    }

    override fun merge(
        current: ProtobufRpcServiceArtifact,
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceArtifact = current
}

private class TextFileAssemblerStub : ArtifactAssembler<TextFileArtifact> {
    override val artifactType: KClass<TextFileArtifact> = TextFileArtifact::class

    override fun create(first: ArtifactContribution<TextFileArtifact>): TextFileArtifact {
        val contribution = first as TextFileArtifactContribution
        return TextFileArtifact(
            id = contribution.artifactId,
            contents = contribution.contents,
        )
    }

    override fun merge(
        current: TextFileArtifact,
        contribution: ArtifactContribution<TextFileArtifact>,
    ): TextFileArtifact = current
}

private class ProtoFileCompilerStub : ArtifactCompiler<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    override fun compile(artifact: ProtoFileArtifact): List<ArtifactContribution<out Artifact>> {
        return emptyList()
    }
}

private class ProtobufRpcCompilerStub : ArtifactCompiler<ProtobufRpcServiceArtifact> {
    override val artifactType: KClass<ProtobufRpcServiceArtifact> = ProtobufRpcServiceArtifact::class

    override fun compile(artifact: ProtobufRpcServiceArtifact): List<ArtifactContribution<out Artifact>> {
        return emptyList()
    }
}

private class TextFileRendererStub : ArtifactRenderer<TextFileArtifact> {
    override val artifactType: KClass<TextFileArtifact> = TextFileArtifact::class

    override fun render(artifact: TextFileArtifact): GeneratedFile = GeneratedFile(
        relativePath = artifact.id.relativePath,
        contents = artifact.contents.toByteArray(),
        outputRoot = artifact.id.outputRoot,
    )
}
