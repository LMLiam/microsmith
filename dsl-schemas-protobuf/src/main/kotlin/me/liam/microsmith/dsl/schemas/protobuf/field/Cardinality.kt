package me.liam.microsmith.dsl.schemas.protobuf.field

enum class Cardinality {
    /**
     * Proto3 "singular" (default). Kept as REQUIRED for backwards compatibility with earlier naming.
     */
    REQUIRED,
    OPTIONAL,
    REPEATED
}
