package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved

public data class Enum(
    override val name: String,
    public val values: List<EnumValue>,
    override val reserved: List<Reserved> = emptyList(),
) : Type, ReservedDeclarationOwner
