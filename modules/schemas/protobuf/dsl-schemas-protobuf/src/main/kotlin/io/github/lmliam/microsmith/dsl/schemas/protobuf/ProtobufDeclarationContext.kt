package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

@MicrosmithDsl
@InternalMicrosmithApi
public interface ProtobufDeclarationContext : ProtobufScope {
    public fun qualifyName(name: String): String

    public fun resolveReference(target: String): String

    public fun registerDeclaration(name: String, declaration: Type)
}
