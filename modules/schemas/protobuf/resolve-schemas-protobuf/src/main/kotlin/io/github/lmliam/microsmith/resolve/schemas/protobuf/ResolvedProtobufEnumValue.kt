package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber

public data class ResolvedProtobufEnumValue(public val name: ProtoIdentifier, public val number: EnumValueNumber)
