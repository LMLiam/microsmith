package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.core.SchemasBuilder
import me.liam.microsmith.dsl.schemas.core.SchemasScope

@MicrosmithDsl
interface ProtobufScope {
    fun message(name: String, block: MessageScope.() -> Unit = {})
    fun enum(name: String, block: EnumScope.() -> Unit = {})
}

interface Messages<TFieldScope : OneofFieldScope> {
    fun int32(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun int64(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun uint32(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun uint64(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun sint32(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun sint64(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun fixed32(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun fixed64(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun sfixed32(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun sfixed64(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun float(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun double(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun string(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun bytes(name: String, block: TFieldScope.() -> Unit = {}): Field
    fun bool(name: String, block: TFieldScope.() -> Unit = {}): Field
}

@MicrosmithDsl
interface MessageScope : Messages<FieldScope> {
    fun optional(field: Field)
    fun optional(block: MessageScope.() -> Field)
    fun repeated(field: Field)
    fun repeated(block: MessageScope.() -> Field)
    fun oneof(name: String, block: OneofScope.() -> Unit)
}

@MicrosmithDsl
interface EnumScope {
    fun value(name: String)
    operator fun String.unaryPlus() = value(this)
}

@MicrosmithDsl
interface OneofScope : Messages<OneofFieldScope>

@MicrosmithDsl
interface FieldScope : OneofFieldScope {
    fun optional()
    fun repeated()
}

@MicrosmithDsl
interface OneofFieldScope {
    fun index(index: Int)
}

fun SchemasScope.protobuf(block: ProtobufScope.() -> Unit) {
    val builder = ProtobufBuilder().apply(block)
    builder.build().forEach { (this as SchemasBuilder).register(it) }
}