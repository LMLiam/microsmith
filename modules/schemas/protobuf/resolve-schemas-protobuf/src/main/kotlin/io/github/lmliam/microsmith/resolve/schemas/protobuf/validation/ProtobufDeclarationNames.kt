package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal data class MessageNames(val fields: List<ProtoIdentifier>, val oneofs: List<OneofNames>)

internal data class OneofNames(val name: ProtoIdentifier, val fields: List<ProtoIdentifier>)
