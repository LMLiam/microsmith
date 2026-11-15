package io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.reserved

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Max
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange

@MicrosmithDsl
public interface ReservedScope {
    public val max: Max
        get() = Max

    public fun index(index: Int)

    public fun name(name: String)

    public fun range(range: IntRange)

    public fun range(range: MaxRange)

    public fun range(start: Int, end: Int) {
        range(start..end)
    }

    public operator fun String.unaryPlus() {
        name(this)
    }

    public operator fun IntRange.unaryPlus() {
        range(this)
    }

    public operator fun MaxRange.unaryPlus() {
        range(this)
    }

    public operator fun Int.rangeTo(max: Max): MaxRange = MaxRange(this)
}
