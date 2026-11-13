package io.github.lmliam.microsmith.compile.schemas.protobuf.render

import io.github.lmliam.microsmith.artifact.schemas.protobuf.model.ProtoFileArtifact

internal object ProtobufFileRenderer {
    private const val PROTO3_SYNTAX_LINE = "syntax = \"proto3\";"

    fun render(artifact: ProtoFileArtifact): String = buildString {
        appendLine(PROTO3_SYNTAX_LINE)
        val packageName = artifact.id.packageName
        packageName?.let { appendLine("package $it;") }
        artifact.imports.forEach { appendLine("import \"$it\";") }
        if (packageName != null || artifact.imports.isNotEmpty()) {
            appendLine()
        }
        artifact.declarations.forEachIndexed { index, declaration ->
            if (index > 0) {
                appendLine()
            }
            appendLine(declaration.contents)
        }
    }
}
