package me.liam.microsmith.dsl.schemas.protobuf.support

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapType
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

internal class ReferenceResolutionContext(schemas: Set<ProtobufSchema>) {
    private val schemasByName = schemas.associateBy(ProtobufSchema::name)
    private val errors = mutableListOf<String>()

    fun resolve(schema: ProtobufSchema): ProtobufSchema {
        val schemaType = schema.schema
        if (schemaType !is Message) {
            return schema
        }

        return schema.copy(schema = schemaType.resolveMessage())
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

    private fun Reference.resolve(context: String): Reference {
        val target = schemasByName[name]?.schema
        if (target == null) {
            errors += "Unresolved reference '$name' in $context"
            return this
        }

        return copy(type = target)
    }

    private fun Field.resolve(messageName: String): Field {
        if (this is ReferenceField) {
            return copy(reference = reference.resolve("message $messageName field '$name'"))
        }
        if (this !is MapField) {
            return this
        }

        val resolvedValue =
            (type.value as? Reference)?.resolve("message $messageName map field '$name' value") ?: type.value
        return copy(type = MapType(type.key, resolvedValue))
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
