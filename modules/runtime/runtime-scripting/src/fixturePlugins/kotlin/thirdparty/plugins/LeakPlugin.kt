package thirdparty.plugins

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import io.github.lmliam.microsmith.resolve.DomainResolution
import io.github.lmliam.microsmith.resolve.DomainResolver
import thirdparty.contract.CollectabilityProbe
import thirdparty.contract.FixtureExtension
import thirdparty.contract.FixtureModel

class LeakPlugin : MicrosmithPlugin {
    override val id = "thirdparty.leak"

    override fun services(host: HostServices): PluginServices {
        CollectabilityProbe.track(this)

        return object : PluginServices {
            override val domainResolvers =
                setOf(
                    object : DomainResolver<FixtureExtension, FixtureModel> {
                        init {
                            CollectabilityProbe.track(this)
                        }

                        override val authoringType = FixtureExtension::class
                        override val resolvedType = FixtureModel::class

                        override fun resolve(authoring: FixtureExtension): DomainResolution<FixtureModel> =
                            DomainResolution.Success(FixtureModel(authoring.text))
                    },
                )

            override val artifactContributors =
                setOf(
                    object : ArtifactContributor<FixtureModel> {
                        init {
                            CollectabilityProbe.track(this)
                        }

                        override val resolvedType = FixtureModel::class

                        override fun contribute(model: FixtureModel): List<ArtifactContribution<Artifact>> = emptyList()
                    },
                )
        }
    }
}
