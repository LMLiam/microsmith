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

@ServiceProvider(ArtifactCompiler::class)
class ProtoFileArtifactCompiler private constructor(private val renderer: ProtobufFileRenderer) :
    SchemasArtifactCompiler<ProtoFileArtifact> {
    constructor() : this(ProtobufFileRenderer())

    override val artifactType = ProtoFileArtifact::class

    override fun compile(artifact: ProtoFileArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(relativePath = artifact.id.relativePath),
                contents = renderer.render(artifact),
            ),
        )
}
