package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

internal fun requireValidReservedNumber(number: Int, label: String) {
    require(number in FieldNumber.RANGE) { "$label must be in ${FieldNumber.RANGE}, but was $number." }
}
