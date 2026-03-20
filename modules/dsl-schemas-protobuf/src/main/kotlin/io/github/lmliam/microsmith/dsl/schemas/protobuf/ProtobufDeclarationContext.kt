package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

@MicrosmithDsl
interface ProtobufDeclarationContext : ProtobufScope {
    fun qualifyName(name: String): String

    fun resolveReference(target: String): String

    fun registerDeclaration(name: String, declaration: Type)
}
