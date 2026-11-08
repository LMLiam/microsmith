package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal fun isIdentifier(raw: String): Boolean = ProtoIdentifier.parse(raw).isRight()

internal fun String.underscoresToCamelCase(capitalizeFirst: Boolean): String =
    split('_').withIndex().joinToString("") { (position, word) ->
        if (position == 0 && !capitalizeFirst) word else word.replaceFirstChar(Char::uppercaseChar)
    }
