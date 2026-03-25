package io.github.lmliam.microsmith.lower.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.lower.core.ArtifactLowerer
import io.github.lmliam.microsmith.lower.schemas.protobuf.render.ProtobufFileRenderer
import java.nio.file.Path
import kotlin.reflect.KClass

@ServiceProvider(ArtifactLowerer::class)
class ProtoFileArtifactLowerer : ArtifactLowerer<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    override fun lower(artifact: ProtoFileArtifact): List<ArtifactContribution<out Artifact>> {
        return listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(relativePath = artifact.relativePath()),
                contents = ProtobufFileRenderer.render(artifact),
            ),
        )
    }

    private fun ProtoFileArtifact.relativePath(): Path {
        val packagePath = id.packageName?.replace('.', '/')
        return if (packagePath == null) {
            Path.of("proto", "${id.typeName}.proto")
        } else {
            Path.of("proto", packagePath, "${id.typeName}.proto")
        }
    }
}
