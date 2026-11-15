package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum.EnumScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.message.MessageScope

@MicrosmithDsl
public interface ProtobufScope {
    public operator fun String.invoke(block: ProtobufScope.() -> Unit)

    public operator fun Int.invoke(block: ProtobufScope.() -> Unit) {
        version(this, block)
    }

    public fun version(version: Int, block: ProtobufScope.() -> Unit)

    public fun message(name: String, block: MessageScope.() -> Unit = {})

    public fun enum(name: String, block: EnumScope.() -> Unit = {})
}
