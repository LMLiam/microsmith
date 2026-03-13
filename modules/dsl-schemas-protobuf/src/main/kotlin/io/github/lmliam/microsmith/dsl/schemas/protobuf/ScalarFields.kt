package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field

interface ScalarFields<TFieldScope : FieldScope, TField : Field> {
    fun int32(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun int64(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun uint32(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun uint64(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun sint32(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun sint64(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun fixed32(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun fixed64(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun sfixed32(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun sfixed64(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun float(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun double(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun string(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun bytes(name: String, block: TFieldScope.() -> Unit = {}): TField

    fun bool(name: String, block: TFieldScope.() -> Unit = {}): TField
}
