package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

internal fun fieldNumber(value: Int): FieldNumber =
    FieldNumber.parse(value).getOrNull() ?: error("$value is not a protobuf field number")
