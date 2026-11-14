package io.github.lmliam.microsmith.gen.errors

class PluginLoadFailedException internal constructor(message: String, cause: Throwable) :
    RuntimeException(message, cause)
