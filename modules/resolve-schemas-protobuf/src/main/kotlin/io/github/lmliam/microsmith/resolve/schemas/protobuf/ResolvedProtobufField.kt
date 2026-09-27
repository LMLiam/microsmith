package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapKeyType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType

sealed interface ResolvedProtobufField {
    val name: String
    val number: Int

    data class Scalar(
        override val name: String,
        override val number: Int,
        val type: PrimitiveType,
        val cardinality: Cardinality,
    ) : ResolvedProtobufField

    data class Reference(
        override val name: String,
        override val number: Int,
        val reference: ResolvedProtobufReference,
        val cardinality: Cardinality,
    ) : ResolvedProtobufField

    data class Map(
        override val name: String,
        override val number: Int,
        val key: MapKeyType,
        val value: ResolvedProtobufValueType,
    ) : ResolvedProtobufField
}
