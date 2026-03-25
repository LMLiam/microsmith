package io.github.lmliam.microsmith.compile.services.dotnet.msbuild

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectArtifact
import io.github.lmliam.microsmith.artifact.services.dotnet.msbuild.MsBuildProjectKind
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.services.core.ServicesArtifactCompiler
import java.nio.file.Path
import kotlin.reflect.KClass

@ServiceProvider(ArtifactCompiler::class)
class MsBuildProjectArtifactCompiler : ServicesArtifactCompiler<MsBuildProjectArtifact> {
    override val artifactType: KClass<MsBuildProjectArtifact> = MsBuildProjectArtifact::class

    override fun compile(artifact: MsBuildProjectArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(
                    relativePath = Path.of(artifact.id.kind.fileName),
                    outputRoot = artifact.outputRoot(),
                ),
                contents = MsBuildProjectXmlRenderer.render(artifact),
            ),
        )
    }

    private fun MsBuildProjectArtifact.outputRoot(): Path = when (id.kind) {
        MsBuildProjectKind.DirectoryPackagesProps -> Path.of("dotnet", id.solutionName)
        MsBuildProjectKind.PackageReferencesProps -> Path.of("dotnet", id.solutionName, requireNotNull(id.projectName))
    }
}
