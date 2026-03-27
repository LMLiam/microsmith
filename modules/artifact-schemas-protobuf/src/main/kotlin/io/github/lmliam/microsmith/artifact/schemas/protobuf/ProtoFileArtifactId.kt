package io.github.lmliam.microsmith.artifact.schemas.protobuf

import io.github.lmliam.microsmith.artifact.core.ArtifactId
import kotlin.reflect.KClass

data class ProtoFileArtifactId(
    val packageName: String?,
    val typeName: String,
) : ArtifactId<ProtoFileArtifact> {
    override val artifactType: KClass<ProtoFileArtifact> = ProtoFileArtifact::class

    val fullyQualifiedName: String = packageName?.let { "$it.$typeName" } ?: typeName
}
