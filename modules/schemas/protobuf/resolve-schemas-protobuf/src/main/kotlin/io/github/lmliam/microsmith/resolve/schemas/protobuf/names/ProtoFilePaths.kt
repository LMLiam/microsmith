package io.github.lmliam.microsmith.resolve.schemas.protobuf.names

object ProtoFilePaths {
    fun importPath(fullyQualifiedName: String): String = fullyQualifiedName.replace('.', '/') + ".proto"
}
