package thirdparty.plugins

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.assembly.ArtifactAssembler
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import thirdparty.common.ClosingServices
import thirdparty.common.StageArtifact
import thirdparty.common.StageArtifactContribution
import thirdparty.contract.ASSEMBLING_CODE
import thirdparty.contract.COMPILING_CODE
import thirdparty.contract.CONTRIBUTING_CODE
import thirdparty.contract.FixtureExtension
import thirdparty.contract.FixtureModel
import thirdparty.contract.RESOLVING_CODE

class StagesPlugin : MicrosmithPlugin {
    override val id = "thirdparty.stages"

    override fun services(host: HostServices): PluginServices =
        object : ClosingServices(host, id) {
            private fun reachedStage(code: String) {
                host.diagnostics.report(Diagnostic(DiagnosticSeverity.INFO, code, id))
            }

            override val domainResolvers =
                setOf(
                    object : DomainResolver<FixtureExtension, FixtureModel> {
                        override val authoringType = FixtureExtension::class
                        override val resolvedType = FixtureModel::class

                        override fun resolve(authoring: FixtureExtension): DomainResolution<FixtureModel> {
                            reachedStage(RESOLVING_CODE)
                            return DomainResolution.Success(FixtureModel(authoring.text))
                        }
                    },
                )

            override val artifactContributors =
                setOf(
                    object : ArtifactContributor<FixtureModel> {
                        override val resolvedType = FixtureModel::class

                        override fun contribute(model: FixtureModel): List<ArtifactContribution<Artifact>> {
                            reachedStage(CONTRIBUTING_CODE)
                            return listOf(StageArtifactContribution(model.text))
                        }
                    },
                )

            override val artifactAssemblers =
                setOf(
                    object : ArtifactAssembler<StageArtifact> {
                        override val artifactType = StageArtifact::class

                        override fun create(contribution: ArtifactContribution<StageArtifact>): StageArtifact {
                            reachedStage(ASSEMBLING_CODE)
                            return StageArtifact((contribution as StageArtifactContribution).text)
                        }

                        override fun merge(
                            current: StageArtifact,
                            contribution: ArtifactContribution<StageArtifact>,
                        ): StageArtifact = current
                    },
                )

            override val artifactCompilers =
                setOf(
                    object : ArtifactCompiler<StageArtifact> {
                        override val artifactType = StageArtifact::class

                        override fun compile(artifact: StageArtifact): List<ArtifactContribution<Artifact>> {
                            reachedStage(COMPILING_CODE)
                            return listOf(
                                TextFileArtifactContribution(
                                    TextFileArtifactId(RelativePath.of("stages.txt")),
                                    "stages:${artifact.text}",
                                ),
                            )
                        }
                    },
                )
        }
}
