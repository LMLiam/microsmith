package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.reserved.ReservedScope

public interface Reservable {
    public fun reserved(vararg indexes: Int)

    public fun reserved(vararg indexRanges: IntRange)

    public fun reserved(vararg names: String)

    public fun reserved(toMax: MaxRange)

    public fun reserved(block: ReservedScope.() -> Unit)
}
