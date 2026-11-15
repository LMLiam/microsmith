package thirdparty.common

import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.ArtifactContributor
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import thirdparty.contract.FixtureModel

open class FileContributor(private val path: String, private val contents: (FixtureModel) -> String) :
    ArtifactContributor<FixtureModel> {
    override val resolvedType = FixtureModel::class

    override fun contribute(model: FixtureModel): List<ArtifactContribution<Artifact>> =
        listOf(TextFileArtifactContribution(TextFileArtifactId(RelativePath.of(path)), contents(model)))
}
