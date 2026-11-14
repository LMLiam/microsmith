package io.github.lmliam.microsmith.plugin

interface Console {
    fun out(line: String)

    fun err(line: String)
}
