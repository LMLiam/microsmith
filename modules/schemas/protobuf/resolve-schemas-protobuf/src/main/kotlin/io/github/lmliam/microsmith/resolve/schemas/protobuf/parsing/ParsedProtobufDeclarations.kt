package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

internal data class ParsedMessage(val fields: List<ParsedField>, val oneofs: List<ParsedOneof>)

internal data class ParsedField(val name: ProtoIdentifier, val number: FieldNumber)

internal data class ParsedOneof(val name: ProtoIdentifier, val fields: List<ParsedField>)

internal data class ParsedEnum(val values: List<ParsedEnumValue>)

internal data class ParsedEnumValue(val name: ProtoIdentifier, val number: EnumValueNumber)
