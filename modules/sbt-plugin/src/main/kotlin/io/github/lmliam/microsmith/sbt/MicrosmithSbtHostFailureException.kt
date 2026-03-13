package io.github.lmliam.microsmith.sbt

class MicrosmithSbtHostFailureException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
