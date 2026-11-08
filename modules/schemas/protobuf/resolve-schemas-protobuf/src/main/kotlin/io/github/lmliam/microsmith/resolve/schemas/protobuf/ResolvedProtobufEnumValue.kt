package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

data class ResolvedProtobufEnumValue(val name: ProtoIdentifier, val number: Int)
