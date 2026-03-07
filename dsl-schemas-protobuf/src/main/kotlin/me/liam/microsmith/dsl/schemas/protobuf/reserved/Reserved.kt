package me.liam.microsmith.dsl.schemas.protobuf.reserved

sealed interface Reserved {
    companion object {
        fun fromRange(range: IntRange): Reserved = when {
            range.first == range.last -> ReservedIndex(range.first)
            range.last == Max.VALUE -> ReservedToMax(range.first)
            else -> ReservedRange(range)
        }
    }
}
