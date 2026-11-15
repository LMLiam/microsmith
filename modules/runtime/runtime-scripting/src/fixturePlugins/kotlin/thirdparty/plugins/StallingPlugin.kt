package thirdparty.plugins

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import thirdparty.contract.CONTRIBUTING_CODE
import thirdparty.contract.FixtureExtension
import thirdparty.contract.FixtureModel
import thirdparty.contract.RESOLVING_CODE

class StallingPlugin : MicrosmithPlugin {
    override val id = "thirdparty.stalling"

    override fun services(host: HostServices): PluginServices =
        object : PluginServices {
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
                            return emptyList()
                        }
                    },
                )
        }
}
