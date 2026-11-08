package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

internal fun requireValidFieldNumber(number: Int, label: String) {
    require(number in FieldNumber.MIN_VALUE..FieldNumber.MAX_VALUE) {
        "$label must be in ${FieldNumber.MIN_VALUE}..${FieldNumber.MAX_VALUE}, but was $number."
    }
    require(number !in FieldNumber.IMPLEMENTATION_RESERVED_RANGE) {
        "$label must not be in reserved range ${FieldNumber.IMPLEMENTATION_RESERVED_RANGE}, but was $number."
    }
}
