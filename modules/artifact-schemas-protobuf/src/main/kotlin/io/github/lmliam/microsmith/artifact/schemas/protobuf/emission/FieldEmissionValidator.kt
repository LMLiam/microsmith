package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal fun validateOneof(oneof: ResolvedProtobufOneof) {
    ProtobufNameValidation.requireIdentifier(oneof.name, "Oneof name")

    require(oneof.fields.isNotEmpty()) {
        "Oneof '${oneof.name}' must contain at least one field"
    }

    oneof.fields.forEach(::validateOneofField)
}

internal fun validateField(field: ResolvedProtobufField) {
    ProtobufNameValidation.requireIdentifier(field.name, "Field name")

    requireValidFieldNumber(field.number, "Field number")
}

private fun validateOneofField(field: ResolvedProtobufOneof.Field) {
    ProtobufNameValidation.requireIdentifier(field.name, "Oneof field name")

    requireValidFieldNumber(field.number, "Oneof field '${field.name}' index")
}
