package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.FieldScope

public interface ScalarFields<TFieldScope : FieldScope, TField : Field> {
    public fun int32(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun int64(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun uint32(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun uint64(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun sint32(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun sint64(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun fixed32(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun fixed64(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun sfixed32(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun sfixed64(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun float(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun double(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun string(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun bytes(name: String, block: TFieldScope.() -> Unit = {}): TField

    public fun bool(name: String, block: TFieldScope.() -> Unit = {}): TField
}
