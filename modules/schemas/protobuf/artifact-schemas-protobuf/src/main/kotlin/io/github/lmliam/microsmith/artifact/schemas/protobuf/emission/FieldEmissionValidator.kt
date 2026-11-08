package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof

internal fun validateOneof(oneof: ResolvedProtobufOneof) {
    require(oneof.fields.isNotEmpty()) { "Oneof '${oneof.name}' must contain at least one field" }

    oneof.fields.forEach(::validateOneofField)
}

internal fun validateField(field: ResolvedProtobufField) {
    requireValidFieldNumber(field.number, "Field number")
}

private fun validateOneofField(field: ResolvedProtobufOneof.Field) {
    requireValidFieldNumber(field.number, "Oneof field '${field.name}' index")
}
