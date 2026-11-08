package io.github.lmliam.microsmith.resolve.schemas.protobuf

internal data class LocatedReference(
    val location: ProtobufResolutionIssue.ReferenceLocation,
    val reference: ResolvedProtobufReference,
)
