package io.github.lmliam.microsmith.cli.plugins.integrity

private const val HEX_SHA256_LENGTH = 64

internal fun isSha256(value: String): Boolean = value.length == HEX_SHA256_LENGTH && value.matches(Regex("^[a-f0-9]+$"))
