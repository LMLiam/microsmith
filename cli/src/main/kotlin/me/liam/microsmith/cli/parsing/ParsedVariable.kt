package me.liam.microsmith.cli.parsing

internal data class ParsedVariable(
    val key: String = "",
    val value: String = "",
    val error: String? = null
)
