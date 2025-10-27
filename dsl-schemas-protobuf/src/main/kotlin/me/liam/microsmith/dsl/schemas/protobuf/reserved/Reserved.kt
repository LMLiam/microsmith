package me.liam.microsmith.dsl.schemas.protobuf.reserved

sealed interface Reserved

data class ReservedIndex(
    val index: Int
) : Reserved

data class ReservedRange(
    val indexRange: IntRange
) : Reserved

data class ReservedName(
    val name: String
) : Reserved

internal fun combineReservedIndexes(indexes: Set<Int>): Set<Reserved> =
    indexes.sorted()
        .fold(mutableListOf<MutableList<Int>>()) { acc, n ->
            when {
                acc.isEmpty() -> acc.apply { add(mutableListOf(n)) }
                n == acc.last().last() + 1 -> acc.apply { acc.last().add(n) }
                else -> acc.apply { add(mutableListOf(n)) }
            }
        }
        .map { group ->
            if (group.size == 1) ReservedIndex(group.first())
            else ReservedRange(group.first()..group.last())
        }
        .toSet()
