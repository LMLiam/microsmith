package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Max
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange

@MicrosmithDsl
interface ReservedScope {
    val max get() = Max

    fun index(index: Int)

    fun name(name: String)

    fun range(range: IntRange)

    fun range(range: MaxRange)

    fun range(start: Int, end: Int) = range(start..end)

    operator fun String.unaryPlus() = name(this)

    operator fun IntRange.unaryPlus() = range(this)

    operator fun MaxRange.unaryPlus() = range(this)

    operator fun Int.rangeTo(max: Max) = MaxRange(this)
}
