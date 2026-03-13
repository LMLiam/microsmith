package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange

interface Reservable {
    fun reserved(vararg indexes: Int)

    fun reserved(vararg indexRanges: IntRange)

    fun reserved(vararg names: String)

    fun reserved(toMax: MaxRange)

    fun reserved(block: ReservedScope.() -> Unit)
}
