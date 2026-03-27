package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal object FieldEmissionValidator {
    fun validate(oneof: Oneof) {
        ProtobufNameValidation.requireIdentifier(oneof.name, "Oneof name")
        require(oneof.fields.isNotEmpty()) { "Oneof '${oneof.name}' must contain at least one field." }
        oneof.fields.forEach(::validate)
    }

    fun validate(field: Field) {
        ProtobufNameValidation.requireIdentifier(field.name, "Field name")
        ProtobufFieldNumbers.requireValidFieldNumber(field.index, "Field '${field.name}' index")
        when (field) {
            is ScalarField -> Unit
            is ReferenceField -> validate(field.reference)
            is MapField -> (field.type.value as? Reference)?.let(::validate)
            is OneofField -> invalidTopLevelOneofField(field.name)
        }
    }

    private fun validate(field: OneofField) {
        ProtobufNameValidation.requireIdentifier(field.name, "Oneof field name")
        ProtobufFieldNumbers.requireValidFieldNumber(field.index, "Oneof field '${field.name}' index")
        (field.fieldType as? Reference)?.let(::validate)
    }

    private fun validate(reference: Reference) {
        ProtobufNameValidation.normalizeQualifiedName(reference.name, "Reference name")
    }
}
