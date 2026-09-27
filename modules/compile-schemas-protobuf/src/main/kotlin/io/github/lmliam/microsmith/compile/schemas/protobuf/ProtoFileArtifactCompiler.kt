package io.github.lmliam.microsmith.compile.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.SchemasArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.protobuf.render.ProtobufFileRenderer
import java.nio.file.Path

@ServiceProvider(ArtifactCompiler::class)
class ProtoFileArtifactCompiler : SchemasArtifactCompiler<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    override fun compile(artifact: ProtoFileArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(relativePath = artifact.relativePath()),
                contents = ProtobufFileRenderer.render(artifact),
                origins = artifact.origins,
            )
        )

    private fun ProtoFileArtifact.relativePath(): Path {
        val packagePath = id.packageName?.replace('.', '/')
        return if (packagePath == null) {
            Path.of("proto", "${id.typeName}.proto")
        } else {
            Path.of("proto", packagePath, "${id.typeName}.proto")
        }
    }
}
