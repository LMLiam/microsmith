package me.liam.microsmith.gen.schemas.protobuf.imports

import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.schemas.protobuf.emission.invalidTopLevelOneofField
import me.liam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation
import me.liam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName

internal fun Message.collectImports(current: QualifiedSchemaName): List<String> =
    buildSet {
        collectReferenceNames().forEach { referenceName ->
            val qualifiedReferenceName = resolveQualifiedReferenceName(referenceName, current)
            if (qualifiedReferenceName != current.fullyQualifiedName) {
                add(qualifiedReferenceName.toImportPath())
            }
        }
    }.toList().sorted()

private fun Message.collectReferenceNames(): List<String> =
    buildList {
        fields.forEach { field ->
            when (field) {
                is ScalarField -> Unit
                is ReferenceField -> add(field.reference.name)
                is MapField -> (field.type.value as? Reference)?.let { add(it.name) }
                is OneofField -> invalidTopLevelOneofField(field.name)
            }
        }

        oneofs.forEach { oneof ->
            oneof.fields.forEach { field ->
                (field.fieldType as? Reference)?.let { add(it.name) }
            }
        }
    }

private fun resolveQualifiedReferenceName(
    referenceName: String,
    current: QualifiedSchemaName
): String {
    val normalized = ProtobufNameValidation.normalizeQualifiedName(referenceName, "Reference name")
    if ('.' in normalized) {
        return normalized
    }
    return current.packageName?.let { "$it.$normalized" } ?: normalized
}

private fun String.toImportPath(): String = replace(".", "/") + ".proto"
