package io.github.lmliam.microsmith.cli.provider

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution.ProtoFileContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.asp.service.DotnetAspServiceArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.references.DotnetPackageReferencesArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.packages.versions.DotnetPackageVersionsArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.dsl.schemas.SchemasExtension
import io.github.lmliam.microsmith.dsl.services.ServicesExtension
import io.github.lmliam.microsmith.gen.plugins.MicrosmithPluginCatalog
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import io.github.lmliam.microsmith.resolve.ResolvedModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcSchemaModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspWorkspace
import io.github.lmliam.microsmith.resolve.services.dotnet.packages.resolution.DotnetPackageWorkspace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

class CliProviderValidatorTests :
    StringSpec(
        {
            "reports missing built-in providers when none are present" {
                val errors = verifyBuiltinProviders(MicrosmithPluginCatalog())

                errors.shouldHaveSize(20)
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
                    "Missing built-in ArtifactAssembler for DotnetPackageVersionsArtifact. " +
                        "Check CLI runtime packaging.",
                )
                errors.shouldContain(
                    "Missing built-in ArtifactAssembler for DotnetPackageReferencesArtifact. " +
                        "Check CLI runtime packaging.",
                )
                errors.shouldContain(
                    "Missing built-in ArtifactAssembler for MsBuildProjectArtifact. Check CLI runtime packaging.",
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
                    "Missing built-in ArtifactCompiler for DotnetPackageReferencesArtifact. " +
                        "Check CLI runtime packaging.",
                )
                errors.shouldContain(
                    "Missing built-in ArtifactCompiler for MsBuildProjectArtifact. Check CLI runtime packaging.",
                )
            }

            "returns no errors when required providers are present" {
                val errors =
                    verifyBuiltinProviders(
                        MicrosmithPluginCatalog(
                            domainResolvers =
                                listOf(
                                    ProtobufResolverStub(),
                                    ProtobufRpcResolverStub(),
                                    DotnetAspResolverStub(),
                                    DotnetPackageResolverStub(),
                                ),
                            artifactContributors =
                                listOf(
                                    ProtobufContributorStub(),
                                    ProtobufRpcContributorStub(),
                                    ContributorStub(DotnetAspWorkspace::class),
                                    ContributorStub(DotnetPackageWorkspace::class),
                                ),
                            artifactAssemblers =
                                listOf(
                                    ProtoFileAssemblerStub(),
                                    ProtobufRpcAssemblerStub(),
                                    AssemblerStub(DotnetAspServiceArtifact::class),
                                    AssemblerStub(DotnetPackageVersionsArtifact::class),
                                    AssemblerStub(DotnetPackageReferencesArtifact::class),
                                    AssemblerStub(MsBuildProjectArtifact::class),
                                ),
                            artifactCompilers =
                                listOf(
                                    CompilerStub(ProtoFileArtifact::class),
                                    CompilerStub(ProtobufRpcServiceArtifact::class),
                                    CompilerStub(DotnetAspServiceArtifact::class),
                                    CompilerStub(DotnetPackageVersionsArtifact::class),
                                    CompilerStub(DotnetPackageReferencesArtifact::class),
                                    CompilerStub(MsBuildProjectArtifact::class),
                                ),
                        ),
                    )

                errors shouldBe emptyList()
            }

            "reports a required plugin id that is not present" {
                val errors =
                    verifyBuiltinProviders(
                        MicrosmithPluginCatalog(),
                        pluginIds = setOf("io.example.present"),
                        requiredPluginIds = listOf("io.example.present", "io.example.absent"),
                    )

                errors shouldContain "Missing built-in plugin 'io.example.absent'. Check CLI runtime packaging."
                errors.filter { it.contains("built-in plugin") } shouldHaveSize 1
            }

            "reports no plugin error when every required plugin id is present" {
                val errors =
                    verifyBuiltinProviders(
                        MicrosmithPluginCatalog(),
                        pluginIds = setOf("io.example.present"),
                        requiredPluginIds = listOf("io.example.present"),
                    )

                errors.filter { it.contains("built-in plugin") } shouldBe emptyList()
            }
        },
    )

private class ProtobufResolverStub : DomainResolver<SchemasExtension, ResolvedProtobufSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufSchemaModel::class

    override fun resolve(authoring: SchemasExtension): DomainResolution<ResolvedProtobufSchemaModel> =
        ResolvedProtobufSchemaModel.of(emptyList())
            .fold(ifLeft = { error("an empty model is valid: $it") }, ifRight = { DomainResolution.Success(it) })
}

private class ProtobufRpcResolverStub : DomainResolver<SchemasExtension, ResolvedProtobufRpcSchemaModel> {
    override val authoringType = SchemasExtension::class
    override val resolvedType = ResolvedProtobufRpcSchemaModel::class

    override fun resolve(authoring: SchemasExtension): DomainResolution<ResolvedProtobufRpcSchemaModel> =
        ResolvedProtobufRpcSchemaModel.of(emptyList())
            .fold(ifLeft = { error("an empty model is valid: $it") }, ifRight = { DomainResolution.Success(it) })
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

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetAspWorkspace> =
        DomainResolution.Success(DotnetAspWorkspace(emptyMap()))
}

private class DotnetPackageResolverStub : DomainResolver<ServicesExtension, DotnetPackageWorkspace> {
    override val authoringType = ServicesExtension::class
    override val resolvedType = DotnetPackageWorkspace::class

    override fun resolve(authoring: ServicesExtension): DomainResolution<DotnetPackageWorkspace> =
        DomainResolution.Success(
            DotnetPackageWorkspace(
                solutionsByName = emptyMap(),
                servicesByName = emptyMap(),
            ),
        )
}

private class ContributorStub<T : ResolvedModel>(override val resolvedType: KClass<T>) : ArtifactContributor<T> {
    override fun contribute(model: T): List<ArtifactContribution<*>> = emptyList()
}

private class ProtoFileAssemblerStub : ArtifactAssembler<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    override fun create(contribution: ArtifactContribution<ProtoFileArtifact>): ProtoFileArtifact {
        val protoContribution = contribution as ProtoFileContribution
        return ProtoFileArtifact(
            id = protoContribution.artifactId,
            imports = protoContribution.imports,
            declarations = protoContribution.declarations,
        )
    }

    override fun merge(
        current: ProtoFileArtifact,
        contribution: ArtifactContribution<ProtoFileArtifact>,
    ): ProtoFileArtifact = current
}

private class ProtobufRpcAssemblerStub : ArtifactAssembler<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    override fun create(contribution: ArtifactContribution<ProtobufRpcServiceArtifact>): ProtobufRpcServiceArtifact {
        error("Not used in provider validator tests.")
    }

    override fun merge(
        current: ProtobufRpcServiceArtifact,
        contribution: ArtifactContribution<ProtobufRpcServiceArtifact>,
    ): ProtobufRpcServiceArtifact = current
}

private class AssemblerStub<T : Artifact>(override val artifactType: KClass<T>) : ArtifactAssembler<T> {
    override fun create(contribution: ArtifactContribution<T>): T {
        error("Not used in provider validator tests.")
    }

    override fun merge(current: T, contribution: ArtifactContribution<T>): T = current
}

private class CompilerStub<T : Artifact>(override val artifactType: KClass<T>) : ArtifactCompiler<T> {
    override fun compile(artifact: T): List<ArtifactContribution<out Artifact>> = emptyList()
}
