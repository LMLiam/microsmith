package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message

internal class ReferenceResolutionContext(schemas: Set<ProtobufSchema>) : ProtobufReferenceResolutionScope {
    private val schemasByName = schemas.associateBy(ProtobufSchema::name)
    private val errors = mutableListOf<String>()

    fun resolve(schema: ProtobufSchema): ProtobufSchema {
        val schemaType = schema.schema
        return when (schemaType) {
            is Message -> schema.copy(schema = schemaType.resolveMessage())
            is ProtobufReferenceAwareType -> schema.copy(schema = schemaType.resolveReferences(this))
            else -> schema
        }
    }

    fun failOnUnresolvedReferences() {
        if (errors.isEmpty()) {
            return
        }

        error(
            buildString {
                appendLine("Unresolved references:")
                errors.forEach { appendLine("- $it") }
            }.trimEnd(),
        )
    }

    override fun resolveReference(reference: Reference, context: String): Reference = reference.resolve(context)

    private fun Reference.resolve(context: String): Reference {
        val target = schemasByName[name]?.schema
        if (target == null) {
            errors += "Unresolved reference '$name' in $context"
            return this
        }

        return copy(type = target)
    }

    private fun Field.resolve(messageName: String): Field = when (this) {
        is ReferenceField -> copy(reference = reference.resolve("message $messageName field '$name'"))

        is MapField -> {
            val resolvedValue =
                (type.value as? Reference)?.resolve("message $messageName map field '$name' value") ?: type.value
            copy(type = MapType(type.key, resolvedValue))
        }

        is ScalarField -> this

        is OneofField -> this
    }

    private fun OneofField.resolve(messageName: String, oneofName: String): OneofField {
        val resolvedFieldType =
            (fieldType as? Reference)?.resolve("message $messageName oneof '$oneofName' field '$name'")
                ?: fieldType
        return copy(fieldType = resolvedFieldType)
    }

    private fun Message.resolveMessage(): Message = copy(
        fields = fields.map { it.resolve(name) },
        oneofs = oneofs.map { it.resolve(name) },
    )

    private fun Oneof.resolve(messageName: String): Oneof = copy(fields = fields.map { it.resolve(messageName, name) })
}
