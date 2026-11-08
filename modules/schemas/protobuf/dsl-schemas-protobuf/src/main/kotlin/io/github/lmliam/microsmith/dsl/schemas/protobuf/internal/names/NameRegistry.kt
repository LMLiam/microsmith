package io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.names

internal class NameRegistry {
    private val used = mutableMapOf<String, String?>()
    private val reserved = mutableSetOf<String>()

    fun reserved() = reserved.toSet()

    fun used() = used.keys.toSet()

    fun use(name: String, explanation: String? = null) {
        validate(name)
        used[name] = explanation
    }

    fun reserve(name: String) {
        validate(name)
        reserved += name
    }

    fun validate(name: String) {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(name !in reserved) { "Name already reserved: $name" }
        require(name !in used) { "Name already used: $name${used[name]?.let { " ($it)" }.orEmpty()}" }
    }
}
