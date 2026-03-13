package io.github.lmliam.microsmith.cli.plugins

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PluginResolutionDiagnosticsTests :
    StringSpec({
        "formats categorized diagnostics with redaction" {
            val formatter = PluginResolutionDiagnostics()
            val diagnostic =
                PluginResolutionDiagnosticException(
                    category = PluginResolverErrorCategory.AUTHENTICATION,
                    message = "Unauthorized for token secret-value",
                )

            formatter.format(
                error = diagnostic,
                sensitiveValues = setOf("secret-value"),
            ) shouldBe "[authentication] Unauthorized for token <redacted>"
        }

        "formats unexpected failures with redaction" {
            val formatter = PluginResolutionDiagnostics()

            formatter.format(
                error = IllegalStateException("Unexpected secret-value failure"),
                sensitiveValues = setOf("secret-value"),
            ) shouldBe "[unexpected] Unexpected <redacted> failure"
        }
    })
