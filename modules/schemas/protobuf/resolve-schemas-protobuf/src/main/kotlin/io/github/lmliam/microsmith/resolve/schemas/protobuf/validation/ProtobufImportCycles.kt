package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal fun findImportCycles(nodes: List<SchemaNode>): List<List<String>> {
    val graph =
        ImportGraph(
            nodes.associate { node ->
                node.identity.fullyQualifiedName to
                    node.dependencies.map(QualifiedSchemaName::fullyQualifiedName).sorted()
            },
        )

    return graph.files
        .map { file -> file to graph.shortestCycleThrough(file) }
        .filter { (file, cycle) -> cycle.isNotEmpty() && file == graph.mutuallyReachableWith(file).min() }
        .map { (_, cycle) -> cycle }
}

private class ImportGraph(declaredImports: Map<String, List<String>>) {
    private val imports = declaredImports.mapValues { (_, targets) -> targets.filter { it in declaredImports } }

    val files = imports.keys.sorted()

    fun shortestCycleThrough(start: String): List<String> {
        val visited = mutableSetOf<String>()
        val paths = ArrayDeque(listOf(listOf(start)))

        while (paths.isNotEmpty()) {
            val path = paths.removeFirst()

            for (next in imports.getValue(path.last())) {
                if (next == start) return path

                if (visited.add(next)) paths.addLast(path + next)
            }
        }

        return emptyList()
    }

    fun mutuallyReachableWith(start: String): Set<String> =
        reachableFrom(start).filter { start in reachableFrom(it) }.toSet() + start

    private fun reachableFrom(start: String): Set<String> {
        val reached = mutableSetOf<String>()
        val pending = ArrayDeque(imports.getValue(start))

        while (pending.isNotEmpty()) {
            val file = pending.removeLast()

            if (reached.add(file)) pending.addAll(imports.getValue(file))
        }

        return reached
    }
}
