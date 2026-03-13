package me.liam.microsmith.sbt

class MicrosmithSbtHostFailureException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
