package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

private val acronymBoundary = Regex("([A-Z]+)([A-Z][a-z])")
private val wordBoundary = Regex("([a-z0-9])([A-Z])")

internal fun unspecifiedValueName(enumName: String): String =
    enumName.replace(acronymBoundary, "$1_$2").replace(wordBoundary, "$1_$2").uppercase() + "_UNSPECIFIED"
