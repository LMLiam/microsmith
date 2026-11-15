package io.github.lmliam.microsmith.artifact.schemas.protobuf.assembly

import io.github.lmliam.microsmith.InternalMicrosmithApi

@InternalMicrosmithApi
public class ProtoFileMerge {
    public fun imports(vararg parts: List<String>): List<String> = parts.flatMap { it }.distinct().sorted()

    public fun <T> members(
        current: List<T>,
        next: List<T>,
        kind: ProtoFileMemberKind,
        owner: String,
        nameOf: (T) -> String,
    ): List<T> {
        val merged = LinkedHashMap(current.associateBy(nameOf))
        next.forEach { member ->
            val existing = merged[nameOf(member)]
            require(existing == null || existing == member) {
                "Conflicting protobuf ${kind.label} '${nameOf(member)}' for '$owner'."
            }
            merged.putIfAbsent(nameOf(member), member)
        }
        return merged.values.toList()
    }
}
