package io.github.lmliam.microsmith.gen.errors

public class PluginLoadFailedException internal constructor(message: String, cause: Throwable) :
    RuntimeException(message, cause)
