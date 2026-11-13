package io.github.lmliam.microsmith.artifact.schemas.protobuf.model

import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import java.nio.file.Path

data class ProtoFileArtifactId(val packageName: String?, val typeName: String) : ArtifactId<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    val fullyQualifiedName: String = packageName?.let { "$it.$typeName" } ?: typeName

    val importPath: String = fullyQualifiedName.replace('.', '/') + ".proto"

    val relativePath: Path = Path.of(PROTO_ROOT, importPath)

    companion object {
        private const val PROTO_ROOT = "proto"

        fun of(name: QualifiedSchemaName) = ProtoFileArtifactId(name.packageName, name.typeName.value)
    }
}
