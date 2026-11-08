package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof

internal fun validateOneof(oneof: ResolvedProtobufOneof) {
    require(oneof.fields.isNotEmpty()) { "Oneof '${oneof.name}' must contain at least one field" }
}
