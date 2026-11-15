package io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved

public sealed interface Reserved {
    public companion object {
        internal fun fromRange(range: IntRange, max: Int): Reserved =
            when {
                range.first == range.last -> ReservedIndex(range.first)
                range.last == max -> ReservedToMax(range.first)
                else -> ReservedRange(range)
            }
    }
}
