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
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.resolve.core.DomainResolver
import io.github.lmliam.microsmith.resolve.core.ResolvedModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.DotnetAspWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.DotnetPackageWorkspace
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

            errors.shouldHaveSize(22)
            errors.shouldContain(
                "Missing built-in DomainResolver for SchemasExtension -> " +
                    "ResolvedProtobufSchemaModel. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in DomainResolver for SchemasExtension -> " +
                    "ResolvedProtobufRpcSchemaModel. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in DomainResolver for ServicesExtension -> " +
                    "DotnetAspWorkspace. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in DomainResolver for ServicesExtension -> " +
                    "DotnetPackageWorkspace. Check CLI runtime packaging.",
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
                "Missing built-in ArtifactContributor for DotnetAspWorkspace. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactContributor for DotnetPackageWorkspace. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for ProtoFileArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for ProtobufRpcServiceArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for DotnetAspServiceArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for DotnetPackageVersionsArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for DotnetPackageReferencesArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactAssembler for MsBuildProjectArtifact. Check CLI runtime packaging.",
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
                "Missing built-in ArtifactCompiler for DotnetAspServiceArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactCompiler for DotnetPackageVersionsArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactCompiler for DotnetPackageReferencesArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactCompiler for MsBuildProjectArtifact. Check CLI runtime packaging.",
            )
            errors.shouldContain(
                "Missing built-in ArtifactRenderer for TextFileArtifact. Check CLI runtime packaging.",
            )
        }

        "returns no errors when required providers are present" {
            val errors =
                verifyBuiltinProviders(
                    domainResolvers = listOf(
                        ProtobufResolverStub(),
                        ProtobufRpcResolverStub(),
                        DotnetAspResolverStub(),
                        DotnetPackageResolverStub(),
                    ),
                    artifactContributors = listOf(
                        ProtobufContributorStub(),
                        ProtobufRpcContributorStub(),
                        ContributorStub(DotnetAspWorkspace::class),
                        ContributorStub(DotnetPackageWorkspace::class),
                    ),
                    artifactAssemblers = listOf(
                        ProtoFileAssemblerStub(),
                        ProtobufRpcAssemblerStub(),
                        AssemblerStub(DotnetAspServiceArtifact::class),
                        AssemblerStub(DotnetPackageVersionsArtifact::class),
                        AssemblerStub(DotnetPackageReferencesArtifact::class),
                        AssemblerStub(MsBuildProjectArtifact::class),
                        TextFileAssemblerStub(),
                    ),
                    artifactCompilers = listOf(
                        CompilerStub(ProtoFileArtifact::class),
                        CompilerStub(ProtobufRpcServiceArtifact::class),
                        CompilerStub(DotnetAspServiceArtifact::class),
                        CompilerStub(DotnetPackageVersionsArtifact::class),
                        CompilerStub(DotnetPackageReferencesArtifact::class),
                        CompilerStub(MsBuildProjectArtifact::class),
                    ),
                    artifactRenderers = listOf(TextFileRendererStub()),
                )

            errors shouldBe emptyList()
        }
    })

private class ProtobufResolverStub : DomainResolver<SchemasExtension, ResolvedProtobufSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedProtobufSchemaModel =
        ResolvedProtobufSchemaModel(emptyList())
}

private class ProtobufRpcResolverStub : DomainResolver<SchemasExtension, ResolvedProtobufRpcSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun resolve(authoring: SchemasExtension): ResolvedProtobufRpcSchemaModel =
        ResolvedProtobufRpcSchemaModel(emptyList())
}

private class ProtobufContributorStub : ArtifactContributor<ResolvedProtobufSchemaModel> {
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun contribute(model: ResolvedProtobufSchemaModel): List<ArtifactContribution<*>> = emptyList()
}

private class ProtobufRpcContributorStub : ArtifactContributor<ResolvedProtobufRpcSchemaModel> {
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun contribute(model: ResolvedProtobufRpcSchemaModel): List<ArtifactContribution<*>> = emptyList()
}

private class DotnetAspResolverStub : DomainResolver<ServicesExtension, DotnetAspWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetAspWorkspace::class

    override fun resolve(authoring: ServicesExtension): DotnetAspWorkspace = DotnetAspWorkspace(emptyMap())
}

private class DotnetPackageResolverStub : DomainResolver<ServicesExtension, DotnetPackageWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetPackageWorkspace::class

    override fun resolve(authoring: ServicesExtension): DotnetPackageWorkspace =
        DotnetPackageWorkspace(emptyMap(), emptyMap())
}

private class ContributorStub<T : ResolvedModel>(override val resolvedType: KClass<T>) : ArtifactContributor<T> {
    override fun contribute(model: T): List<ArtifactContribution<*>> = emptyList()
}

private class ProtoFileAssemblerStub : ArtifactAssembler<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

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
    override val artifactType = ProtobufRpcServiceArtifact::class

    override fun create(first: ArtifactContribution<ProtobufRpcServiceArtifact>): ProtobufRpcServiceArtifact {
        error("Not used in provider validator tests.")
    }

    override fun merge(
        current: ProtobufRpcServiceArtifact,
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceArtifact = current
}

private class AssemblerStub<T : Artifact>(override val artifactType: KClass<T>) : ArtifactAssembler<T> {
    override fun create(first: ArtifactContribution<T>): T {
        error("Not used in provider validator tests.")
    }

    override fun merge(current: T, contribution: ArtifactContribution<T>): T = current
}

private class TextFileAssemblerStub : ArtifactAssembler<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

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

private class CompilerStub<T : Artifact>(override val artifactType: KClass<T>) : ArtifactCompiler<T> {
    override fun compile(artifact: T): List<ArtifactContribution<out Artifact>> = emptyList()
}

private class TextFileRendererStub : ArtifactRenderer<TextFileArtifact> {
    override val artifactType = TextFileArtifact::class

    override fun render(artifact: TextFileArtifact): GeneratedFile = GeneratedFile(
        relativePath = artifact.id.relativePath,
        contents = artifact.contents.toByteArray(),
        outputRoot = artifact.id.outputRoot,
    )
}
