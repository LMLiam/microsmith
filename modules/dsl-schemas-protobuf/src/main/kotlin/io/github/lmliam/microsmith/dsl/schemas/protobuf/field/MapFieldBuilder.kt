package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.reference.textualReference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.MapFieldScope

internal class MapFieldBuilder(var index: Int? = null, var key: MapKeyType? = null, var value: ValueType? = null) :
    MapFieldScope {
    override fun index(index: Int) {
        this.index = index
    }

    override fun key(keyType: MapKeyType) {
        require(this.key == null) { "Key already set to ${this.key}" }
        this.key = keyType
    }

    override fun value(valueType: ValueType) {
        require(this.value == null) { "Value already set to ${this.value}" }
        this.value = valueType
    }

    override fun types(kvpValue: Pair<MapKeyType, ValueType>) {
        key(kvpValue.first)
        value(kvpValue.second)
    }

    override fun ref(target: String): Reference = textualReference(target)
}
