package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved

data class Message(
    override val name: String,
    val fields: List<Field> = emptyList(),
    val oneofs: List<Oneof> = emptyList(),
    override val reserved: List<Reserved> = emptyList(),
) : Type, ReservedDeclarationOwner
