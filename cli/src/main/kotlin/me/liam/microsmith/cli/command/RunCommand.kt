package me.liam.microsmith.cli.command

import java.nio.file.Path

internal data class RunCommand(
    val script: Path,
    val outputDir: Path
) : CliCommand
