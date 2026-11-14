package io.github.lmliam.microsmith.plugin

interface DiagnosticSink {
    fun info(message: String)

    fun warn(message: String)
}
