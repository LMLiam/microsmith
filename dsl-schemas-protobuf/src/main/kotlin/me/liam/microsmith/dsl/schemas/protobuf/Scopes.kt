package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.core.SchemasBuilder
import me.liam.microsmith.dsl.schemas.core.SchemasScope

@MicrosmithDsl
interface ProtobufScope {
    fun message(name: String, block: MessageScope.() -> Unit = {})
}

@MicrosmithDsl
interface MessageScope {
    fun int32(name: String, block: FieldScope.() -> Unit = {})
    fun string(name: String, block: FieldScope.() -> Unit = {})
}

@MicrosmithDsl
interface FieldScope {
    fun optional()
    fun index(index: Int)
}

fun SchemasScope.protobuf(block: ProtobufScope.() -> Unit) {
    val builder = ProtobufBuilder().apply(block)
    builder.build().forEach { (this as SchemasBuilder).register(it) }
}