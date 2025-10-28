package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.MapFieldScope
import me.liam.microsmith.dsl.schemas.protobuf.support.getReferencePath

class MapFieldBuilder(
    private val segments: List<String>,
    var index: Int? = null,
    var key: MapKeyType? = null,
    var value: ValueType? = null,
) : MapFieldScope {
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

    override fun ref(target: String): Reference {
        val fqName = getReferencePath(segments, target).joinToString(".")
        return Reference(fqName)
    }
}