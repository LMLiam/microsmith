package io.github.lmliam.microsmith.cli.diagnostics

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class CliDiagnosticEmitterTests :
    StringSpec({
        "text diagnostics write info to stdout and warn/error to stderr" {
            val stdout = mutableListOf<String>()
            val stderr = mutableListOf<String>()
            val emitter =
                CliDiagnosticEmitter(
                    format = DiagnosticFormat.TEXT,
                    verbose = false,
                    stdout = stdout::add,
                    stderr = stderr::add,
                )

            emitter.info("info message")
            emitter.warn("warn message")
            emitter.error(CliFailureCode.USAGE_ERROR, "error message")

            stdout shouldBe listOf("[info] info message")
            stderr shouldBe listOf(
                "[warn] warn message",
                "[error] [MS-CLI-0001] error message",
            )
        }

        "verbose text diagnostics sort details by key" {
            val stdout = mutableListOf<String>()
            val emitter =
                CliDiagnosticEmitter(
                    format = DiagnosticFormat.TEXT,
                    verbose = true,
                    stdout = stdout::add,
                    stderr = { error("stderr should not be used") },
                )

            emitter.info(
                "info message",
                details = mapOf("zeta" to "last", "alpha" to "first"),
            )

            stdout shouldBe listOf(
                "[info] info message",
                "  alpha=first",
                "  zeta=last",
            )
        }

        "json diagnostics only include details when verbose" {
            val nonVerbose = mutableListOf<String>()
            val verbose = mutableListOf<String>()
            val nonVerboseEmitter =
                CliDiagnosticEmitter(
                    format = DiagnosticFormat.JSON,
                    verbose = false,
                    stdout = nonVerbose::add,
                    stderr = nonVerbose::add,
                )
            val verboseEmitter =
                CliDiagnosticEmitter(
                    format = DiagnosticFormat.JSON,
                    verbose = true,
                    stdout = verbose::add,
                    stderr = verbose::add,
                )

            nonVerboseEmitter.error(
                CliFailureCode.PLUGIN_RESOLUTION_FAILED,
                "resolver failed",
                details = mapOf("b" to "two", "a" to "one"),
            )
            verboseEmitter.error(
                CliFailureCode.PLUGIN_RESOLUTION_FAILED,
                "resolver failed",
                details = mapOf("b" to "two", "a" to "one"),
            )

            nonVerbose.single().shouldContain("\"code\":\"MS-CLI-1101\"")
            nonVerbose.single().shouldContain("\"message\":\"resolver failed\"")
            nonVerbose.single().shouldContain("\"level\":\"error\"")
            nonVerbose.single().contains("\"details\"") shouldBe false

            verbose.single().shouldContain("\"details\":{\"a\":\"one\",\"b\":\"two\"}")
        }
    })
