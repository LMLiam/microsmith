package io.github.lmliam.microsmith.compile.schemas.protobuf

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactContribution
import io.github.lmliam.microsmith.artifact.files.TextFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.SchemasArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.protobuf.render.ProtobufFileRenderer
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufPluginScope

@Inject
@ContributesIntoSet(ProtobufPluginScope::class, binding = binding<ArtifactCompiler<*>>())
internal class ProtoFileArtifactCompiler(private val renderer: ProtobufFileRenderer) :
    SchemasArtifactCompiler<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    override fun compile(artifact: ProtoFileArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            TextFileArtifactContribution(
                artifactId = TextFileArtifactId(relativePath = artifact.id.relativePath),
                contents = renderer.render(artifact),
            ),
        )
}
