package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Cardinality
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapKeyType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

public sealed interface ResolvedProtobufField {
    public val name: ProtoIdentifier
    public val number: FieldNumber

    public data class Scalar(
        override val name: ProtoIdentifier,
        override val number: FieldNumber,
        public val type: PrimitiveType,
        public val cardinality: Cardinality,
    ) : ResolvedProtobufField

    public data class Reference(
        override val name: ProtoIdentifier,
        override val number: FieldNumber,
        public val reference: ResolvedProtobufReference,
        public val cardinality: Cardinality,
    ) : ResolvedProtobufField

    public data class Map(
        override val name: ProtoIdentifier,
        override val number: FieldNumber,
        public val key: MapKeyType,
        public val value: ResolvedProtobufValueType,
    ) : ResolvedProtobufField
}
