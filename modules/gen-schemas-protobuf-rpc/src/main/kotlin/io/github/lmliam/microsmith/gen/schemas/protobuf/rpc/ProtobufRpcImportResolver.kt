package io.github.lmliam.microsmith.gen.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.Service
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation
import io.github.lmliam.microsmith.gen.schemas.protobuf.names.QualifiedSchemaName

internal fun Service.collectImports(current: QualifiedSchemaName): List<String> = buildSet {
    rpcs.flatMap { listOf(it.request.reference, it.response.reference) }.forEach { reference ->
        val qualifiedReferenceName = resolveQualifiedReferenceName(reference, current)
        if (qualifiedReferenceName != current.fullyQualifiedName) {
            add(qualifiedReferenceName.replace('.', '/') + ".proto")
        }
    }
}.toList().sorted()

private fun resolveQualifiedReferenceName(reference: Reference, current: QualifiedSchemaName): String {
    val normalized = ProtobufNameValidation.normalizeQualifiedName(reference.name, "RPC reference name")
    if ('.' in normalized) {
        return normalized
    }
    return current.packageName?.let { "$it.$normalized" } ?: normalized
}
