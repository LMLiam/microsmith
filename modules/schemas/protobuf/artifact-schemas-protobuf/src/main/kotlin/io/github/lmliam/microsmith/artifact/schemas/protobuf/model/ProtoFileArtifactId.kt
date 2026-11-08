package io.github.lmliam.microsmith.artifact.schemas.protobuf.model

import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoFilePaths
import java.nio.file.Path

data class ProtoFileArtifactId(val packageName: String?, val typeName: String) : ArtifactId<ProtoFileArtifact> {
    override val artifactType = ProtoFileArtifact::class

    val fullyQualifiedName: String = packageName?.let { "$it.$typeName" } ?: typeName

    val importPath: String = ProtoFilePaths.importPath(fullyQualifiedName)

    val relativePath: Path = Path.of(PROTO_ROOT, importPath)

    private companion object {
        const val PROTO_ROOT = "proto"
    }
}
