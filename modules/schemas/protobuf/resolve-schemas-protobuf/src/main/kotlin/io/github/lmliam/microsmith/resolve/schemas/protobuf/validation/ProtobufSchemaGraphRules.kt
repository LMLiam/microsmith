package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchemaModel

internal fun schemaGraphRejections(nodes: List<SchemaNode>): List<ResolvedProtobufSchemaModel.Rejection> {
    val duplicates =
        nodes
            .groupingBy { it.identity }
            .eachCount()
            .filterValues { it > 1 }
            .map { (identity, occurrences) ->
                ResolvedProtobufSchemaModel.Rejection.DuplicateSchema(identity, occurrences)
            }

    return duplicates + findImportCycles(nodes).map(ResolvedProtobufSchemaModel.Rejection::CircularImports)
}
