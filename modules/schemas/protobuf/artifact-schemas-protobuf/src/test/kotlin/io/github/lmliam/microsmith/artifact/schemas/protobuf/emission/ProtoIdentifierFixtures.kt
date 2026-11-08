package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal fun protoIdentifier(value: String): ProtoIdentifier =
    ProtoIdentifier.parse(value).getOrNull() ?: error("'$value' is not a protobuf identifier")
