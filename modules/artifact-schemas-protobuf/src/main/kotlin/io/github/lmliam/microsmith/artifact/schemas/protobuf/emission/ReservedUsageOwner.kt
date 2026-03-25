package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

internal data class ReservedUsageOwner(
    val kind: Kind,
    val name: String,
) {
    val displayName: String
        get() = kind.displayName

    enum class Kind(
        val displayName: String,
    ) {
        MESSAGE("Message"),
        ENUM("Enum"),
    }

    companion object {
        fun message(name: String): ReservedUsageOwner = ReservedUsageOwner(kind = Kind.MESSAGE, name = name)

        fun enum(name: String): ReservedUsageOwner = ReservedUsageOwner(kind = Kind.ENUM, name = name)
    }
}
