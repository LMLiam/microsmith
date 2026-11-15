package io.github.lmliam.microsmith.gen.errors

import arrow.core.NonEmptyList
import io.github.lmliam.microsmith.resolve.ResolutionIssue
import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic

public class GenerationResolutionFailedException(
    public val issues: NonEmptyList<ResolutionIssue>,
    public val diagnostics: NonEmptyList<ResolutionDiagnostic>,
) :
    RuntimeException(
        buildString {
            append("Microsmith model resolution failed with ${issues.size} semantic issue(s): ")

            append(diagnostics.joinToString("; ") { "[${it.code}] ${it.message}" })
        },
    )
