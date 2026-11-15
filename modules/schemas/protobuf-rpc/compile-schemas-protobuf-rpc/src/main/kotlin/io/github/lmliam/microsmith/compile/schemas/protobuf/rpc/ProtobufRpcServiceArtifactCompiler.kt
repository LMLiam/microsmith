package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc

import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import io.github.lmliam.microsmith.artifact.Artifact
import io.github.lmliam.microsmith.artifact.ArtifactContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.contribution.ProtoFileContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.compile.ArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.SchemasArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.protobuf.rpc.render.ProtobufServiceRenderer
import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ProtobufRpcPluginScope

@Inject
@ContributesIntoSet(ProtobufRpcPluginScope::class, binding = binding<ArtifactCompiler<*>>())
internal class ProtobufRpcServiceArtifactCompiler(private val renderer: ProtobufServiceRenderer) :
    SchemasArtifactCompiler<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    override fun compile(artifact: ProtobufRpcServiceArtifact): List<ArtifactContribution<out Artifact>> =
        listOf(
            ProtoFileContribution(
                artifactId = artifact.id.protoFile,
                imports = artifact.imports,
                declarations =
                    listOf(
                        ProtoDeclaration(
                            name = artifact.id.serviceName,
                            contents = renderer.render(artifact),
                        ),
                    ),
            ),
        )
}
