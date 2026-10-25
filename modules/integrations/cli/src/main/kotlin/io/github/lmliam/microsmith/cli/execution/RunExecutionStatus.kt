package io.github.lmliam.microsmith.cli.execution

internal enum class RunExecutionStatus(val wireValue: String) {
    SKIPPED("skipped"),
    SUCCESS("success"),
    FAILURE("failure"),
}
