package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapKeyType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

sealed interface ResolvedProtobufField {
    val name: ProtoIdentifier
    val number: FieldNumber

    data class Scalar(
        override val name: ProtoIdentifier,
        override val number: FieldNumber,
        val type: PrimitiveType,
        val cardinality: Cardinality,
    ) : ResolvedProtobufField

    data class Reference(
        override val name: ProtoIdentifier,
        override val number: FieldNumber,
        val reference: ResolvedProtobufReference,
        val cardinality: Cardinality,
    ) : ResolvedProtobufField

    data class Map(
        override val name: ProtoIdentifier,
        override val number: FieldNumber,
        val key: MapKeyType,
        val value: ResolvedProtobufValueType,
    ) : ResolvedProtobufField
}
