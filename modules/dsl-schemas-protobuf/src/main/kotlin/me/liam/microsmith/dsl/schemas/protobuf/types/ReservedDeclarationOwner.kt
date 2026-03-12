package me.liam.microsmith.dsl.schemas.protobuf.types

import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved

/**
 * Marker for protobuf model declarations that can own reserved declarations.
 *
 * In protobuf, `reserved` blocks are supported by messages and enums.
 */
interface ReservedDeclarationOwner {
    val reserved: List<Reserved>
}
