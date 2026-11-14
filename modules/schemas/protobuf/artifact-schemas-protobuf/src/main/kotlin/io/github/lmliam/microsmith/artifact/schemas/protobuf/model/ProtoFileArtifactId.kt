package io.github.lmliam.microsmith.artifact.schemas.protobuf.model

import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

data class ProtoFileArtifactId(val packageName: String?, val typeName: String) : ArtifactId<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    val fullyQualifiedName: String = packageName?.let { "$it.$typeName" } ?: typeName

    val importPath: String = fullyQualifiedName.replace('.', '/') + ".proto"

    val relativePath: RelativePath = RelativePath.of(PROTO_ROOT, importPath)

    companion object {
        private const val PROTO_ROOT = "proto"

        fun of(name: QualifiedSchemaName) = ProtoFileArtifactId(name.packageName, name.typeName.value)
    }
}
