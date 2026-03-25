package io.github.lmliam.microsmith.gen.schemas.protobuf

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifact
import io.github.lmliam.microsmith.gen.core.ArtifactRenderer
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.gen.schemas.protobuf.render.ProtobufFileRenderer
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

@ServiceProvider(ArtifactRenderer::class)
class ProtoFileArtifactRenderer : ArtifactRenderer<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    override fun render(artifact: ProtoFileArtifact): GeneratedFile = GeneratedFile(
        relativePath = artifact.id.relativePath,
        contents = ProtobufFileRenderer.render(artifact).toByteArray(StandardCharsets.UTF_8),
        outputRoot = artifact.id.outputRoot,
    )
}
