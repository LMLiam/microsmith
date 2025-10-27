package me.liam.microsmith.dsl.schemas.protobuf.reserved

sealed interface Reserved

data class ReservedIndex(
    val index: Int
) : Reserved

data class ReservedToMax(
    val from: Int
) : Reserved

data class ReservedRange(
    val indexRange: IntRange
) : Reserved

data class ReservedName(
    val name: String
) : Reserved
