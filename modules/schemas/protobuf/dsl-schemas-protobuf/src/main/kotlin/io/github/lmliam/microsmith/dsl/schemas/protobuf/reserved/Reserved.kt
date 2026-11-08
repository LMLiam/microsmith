package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

sealed interface Reserved {
    companion object {
        fun fromRange(range: IntRange, max: Int): Reserved =
            when {
                range.first == range.last -> ReservedIndex(range.first)
                range.last == max -> ReservedToMax(range.first)
                else -> ReservedRange(range)
            }
    }
}
