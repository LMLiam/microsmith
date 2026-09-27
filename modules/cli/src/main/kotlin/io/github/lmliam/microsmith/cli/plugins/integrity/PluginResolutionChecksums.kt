package io.github.lmliam.microsmith.cli.plugins.integrity

import io.github.lmliam.microsmith.cli.plugins.integrity.sha256 as fileSha256
import java.nio.file.Path

private const val HEX_SHA256_LENGTH = 64

internal fun sha256(path: Path): String = fileSha256(path)

internal fun isSha256(value: String): Boolean = value.length == HEX_SHA256_LENGTH && value.matches(Regex("^[a-f0-9]+$"))
