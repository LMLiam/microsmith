package thirdparty.plugins

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.plugin.HostServices
import io.github.lmliam.microsmith.plugin.MicrosmithPlugin
import io.github.lmliam.microsmith.plugin.PluginServices
import thirdparty.common.ClosingServices
import thirdparty.contract.FixtureModel

class BulkPlugin : MicrosmithPlugin {
    override val id = "thirdparty.bulk"

    override fun services(host: HostServices): PluginServices =
        object : ClosingServices(host, id) {
            override val artifactContributors =
                setOf(
                    object : ArtifactContributor<FixtureModel> {
                        override val resolvedType = FixtureModel::class

                        override fun contribute(model: FixtureModel): List<ArtifactContribution<Artifact>> =
                            (0 until FILE_COUNT).map { index ->
                                TextFileArtifactContribution(
                                    TextFileArtifactId(
                                        RelativePath.of("bulk", "group${index % GROUPS}", "file$index.txt"),
                                    ),
                                    "bulk:$index:${model.text}",
                                )
                            }
                    },
                )
        }

    private companion object {
        const val FILE_COUNT = 64
        const val GROUPS = 4
    }
}
