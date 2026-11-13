package io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly

object ProtoFileMerge {
    fun imports(vararg parts: List<String>): List<String> = parts.flatMap { it }.distinct().sorted()

    fun <T> members(
        current: List<T>,
        next: List<T>,
        kind: String,
        owner: String,
        nameOf: (T) -> String,
    ): List<T> {
        val merged = LinkedHashMap(current.associateBy(nameOf))
        next.forEach { member ->
            val existing = merged[nameOf(member)]
            require(existing == null || existing == member) {
                "Conflicting protobuf $kind '${nameOf(member)}' for '$owner'."
            }
            merged.putIfAbsent(nameOf(member), member)
        }
        return merged.values.toList()
    }
}
