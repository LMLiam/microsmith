package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber

data class ResolvedProtobufEnumValue(val name: ProtoIdentifier, val number: EnumValueNumber)
