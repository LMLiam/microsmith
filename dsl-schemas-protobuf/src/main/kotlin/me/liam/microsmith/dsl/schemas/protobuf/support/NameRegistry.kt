package me.liam.microsmith.dsl.schemas.protobuf.support

class NameRegistry {
    private val used = mutableSetOf<String>()
    private val reserved = mutableSetOf<String>()

    fun reserved() = reserved.toSet()

    fun used() = used.toSet()

    fun use(name: String) {
        validate(name)
        used += name
    }

    fun reserve(name: String) {
        validate(name)
        reserved += name
    }

    fun validate(name: String) {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(name !in reserved) { "Name already reserved: $name" }
        require(name !in used) { "Name already used: $name" }
    }
}