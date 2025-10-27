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
    fun optional(field: Field)

    fun int32(name: String, block: FieldScope.() -> Unit = {}): Field
    fun string(name: String, block: FieldScope.() -> Unit = {}): Field
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