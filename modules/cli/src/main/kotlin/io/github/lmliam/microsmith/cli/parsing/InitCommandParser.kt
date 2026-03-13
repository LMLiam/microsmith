package io.github.lmliam.microsmith.cli.parsing

import io.github.lmliam.microsmith.cli.command.CliCommand
import io.github.lmliam.microsmith.cli.command.ErrorCommand
import io.github.lmliam.microsmith.cli.command.InitCommand
import java.nio.file.Path

internal fun parseInitCommand(args: List<String>): CliCommand {
    val parsed = parseInitOptions(args = args, startIndex = 1)
    parsed.error?.let { return ErrorCommand(it) }
    return InitCommand(
        projectRoot = parsed.projectRoot,
        diagnosticsFormat = parsed.diagnosticsFormat,
        verbose = parsed.verbose,
        force = parsed.force,
        skipIdeHelper = parsed.skipIdeHelper,
    )
}

private fun parseInitOptions(args: List<String>, startIndex: Int): ParsedInitOptions {
    val diagnosticOptions = DiagnosticOptionsState()
    var projectRoot = Path.of(".")
    var projectRootSpecified = false
    var force = false
    var skipIdeHelper = false
    var error: String? = null
    var index = startIndex

    while (index < args.size && error == null && diagnosticOptions.error == null) {
        val parsedToken =
            when (val token = args[index]) {
                DIAGNOSTICS_OPTION -> ParsedToken(
                    index + diagnosticOptions.consumeDiagnostics(args = args, index = index),
                )
                VERBOSE_OPTION ->
                    ParsedToken(
                        index + diagnosticOptions.consumeVerbose(),
                    )
                REPO_ROOT_OPTION ->
                    parseRepoRootOption(
                        args = args,
                        index = index,
                        alreadySpecified = projectRootSpecified,
                    ) { parsedProjectRoot ->
                        projectRoot = parsedProjectRoot
                        projectRootSpecified = true
                    }
                FORCE_OPTION ->
                    parseSingleOccurrenceFlag(
                        index = index,
                        alreadySpecified = force,
                        optionName = "--force",
                    ) {
                        force = true
                    }
                SKIP_IDE_HELPER_OPTION ->
                    parseSingleOccurrenceFlag(
                        index = index,
                        alreadySpecified = skipIdeHelper,
                        optionName = "--skip-ide-helper",
                    ) {
                        skipIdeHelper = true
                    }
                else -> ParsedToken(nextIndex = index, error = "Unknown option '$token'.")
            }
        if (parsedToken.error != null) {
            error = parsedToken.error
        }
        if (parsedToken.nextIndex <= index) {
            break
        }
        index = parsedToken.nextIndex
    }

    return ParsedInitOptions(
        projectRoot = projectRoot,
        diagnosticsFormat = diagnosticOptions.diagnosticsFormat,
        verbose = diagnosticOptions.verbose,
        force = force,
        skipIdeHelper = skipIdeHelper,
        error = diagnosticOptions.error ?: error,
    )
}
