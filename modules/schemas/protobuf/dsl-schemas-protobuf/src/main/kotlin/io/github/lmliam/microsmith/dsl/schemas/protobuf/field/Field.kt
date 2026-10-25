package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

sealed interface Field {
    val name: String
    val index: Int
}
