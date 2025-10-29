package me.liam.microsmith.dsl.schemas.protobuf.reserved

object Max {
    internal const val VALUE = 536_870_911
}

data class MaxRange(
    val from: Int
)