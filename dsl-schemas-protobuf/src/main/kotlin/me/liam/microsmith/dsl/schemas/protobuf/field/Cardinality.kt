package me.liam.microsmith.dsl.schemas.protobuf.field

enum class Cardinality {
    REQUIRED, // Default in proto3
    OPTIONAL,
    REPEATED
}