package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal fun isIdentifier(raw: String): Boolean = ProtoIdentifier.parse(raw).isRight()
