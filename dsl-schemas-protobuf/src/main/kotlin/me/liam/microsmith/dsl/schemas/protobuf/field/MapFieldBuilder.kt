package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.MapFieldScope

class MapFieldBuilder(
    var index: Int? = null,
    var key: MapKeyType? = null,
    var value: MapValueType? = null,
) : MapFieldScope {
    override fun index(index: Int) {
        this.index = index
    }

    override fun key(keyType: MapKeyType) {
        require(this.key == null) { "Key already set to ${this.key}" }
        this.key = keyType
    }

    override fun value(valueType: MapValueType) {
        require(this.value == null) { "Value already set to ${this.value}" }
        this.value = valueType
    }

    override fun types(kvp: Pair<MapKeyType, MapValueType>) {
        key(kvp.first)
        value(kvp.second)
    }
}