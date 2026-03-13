package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface ProtobufScope {
    operator fun String.invoke(block: ProtobufScope.() -> Unit)

    operator fun Int.invoke(block: ProtobufScope.() -> Unit) = version(this, block)

    fun version(version: Int, block: ProtobufScope.() -> Unit)

    fun message(name: String, block: MessageScope.() -> Unit = {})

    fun enum(name: String, block: EnumScope.() -> Unit = {})
}
