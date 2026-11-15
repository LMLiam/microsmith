package io.github.lmliam.microsmith.artifact.schemas.protobuf.model

import io.github.lmliam.microsmith.artifact.ArtifactId
import io.github.lmliam.microsmith.artifact.files.RelativePath
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import kotlin.reflect.KClass

public data class ProtoFileArtifactId(public val packageName: String?, public val typeName: String) :
    ArtifactId<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    public val fullyQualifiedName: String = packageName?.let { "$it.$typeName" } ?: typeName

    public val importPath: String = fullyQualifiedName.replace('.', '/') + ".proto"

    public val relativePath: RelativePath = RelativePath.of(PROTO_ROOT, importPath)

    public companion object {
        private const val PROTO_ROOT = "proto"

        public fun of(name: QualifiedSchemaName): ProtoFileArtifactId =
            ProtoFileArtifactId(name.packageName, name.typeName.value)
    }
}
