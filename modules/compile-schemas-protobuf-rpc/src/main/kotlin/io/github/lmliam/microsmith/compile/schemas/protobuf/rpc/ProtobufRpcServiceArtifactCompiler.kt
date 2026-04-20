package io.github.lmliam.microsmith.compile.schemas.protobuf.rpc

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.artifact.core.Artifact
import io.github.lmliam.microsmith.artifact.core.ArtifactContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoDeclaration
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileArtifactId
import io.github.lmliam.microsmith.artifact.schemas.protobuf.ProtoFileContribution
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifact
import io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc.ProtobufRpcServiceArtifactId
import io.github.lmliam.microsmith.compile.core.ArtifactCompiler
import io.github.lmliam.microsmith.compile.schemas.core.SchemasArtifactCompiler

@ServiceProvider(ArtifactCompiler::class)
class ProtobufRpcServiceArtifactCompiler : SchemasArtifactCompiler<ProtobufRpcServiceArtifact> {
    override val artifactType = ProtobufRpcServiceArtifact::class

    override fun compile(artifact: ProtobufRpcServiceArtifact): List<ArtifactContribution<out Artifact>> = listOf(
        ProtoFileContribution(
            artifactId = ProtoFileArtifactId(
                packageName = artifact.id.packageName,
                typeName = artifact.id.serviceName,
            ),
            packageName = artifact.id.packageName,
            imports = artifact.imports,
            declarations = listOf(
                ProtoDeclaration(
                    name = artifact.id.serviceName,
                    contents = ProtobufServiceRenderer.render(artifact),
                ),
            ),
            origins = buildSet {
                add(artifact.id.qualifiedName("service"))
                artifact.operations.forEach { operation ->
                    add(artifact.id.qualifiedName(operation.name))
                }
            },
        ),
    )

    private fun ProtobufRpcServiceArtifactId.qualifiedName(suffix: String): String = buildString {
        append("schemas.protobuf")
        packageName?.let { append('.').append(it) }
        append('.').append(serviceName)
        append('.').append(suffix)
    }
}
