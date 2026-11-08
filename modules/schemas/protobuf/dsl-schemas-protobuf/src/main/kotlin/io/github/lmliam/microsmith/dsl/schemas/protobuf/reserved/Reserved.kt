package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

import io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers.ProtobufNumberLimits

sealed interface Reserved {
    companion object {
        fun fromRange(range: IntRange): Reserved =
            when {
                range.first == range.last -> ReservedIndex(range.first)
                range.last == ProtobufNumberLimits.FIELD_MAX -> ReservedToMax(range.first)
                else -> ReservedRange(range)
            }
    }
}
