package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MessageField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved

public data class Message(
    override val name: String,
    public val fields: List<MessageField> = emptyList(),
    public val oneofs: List<Oneof> = emptyList(),
    override val reserved: List<Reserved> = emptyList(),
) : Type, ReservedDeclarationOwner
