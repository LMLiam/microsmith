package io.github.lmliam.microsmith.resolve.schemas.protobuf

import arrow.core.NonEmptyList
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

public data class ResolvedProtobufOneof(public val name: ProtoIdentifier, public val fields: NonEmptyList<Field>) {
    public data class Field(
        public val name: ProtoIdentifier,
        public val number: FieldNumber,
        public val type: ResolvedProtobufValueType,
    )
}
