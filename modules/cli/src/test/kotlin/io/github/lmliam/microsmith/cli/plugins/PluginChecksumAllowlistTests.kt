package io.github.lmliam.microsmith.cli.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginChecksumAllowlistTests :
    StringSpec({
        "loadPluginChecksumAllowlistFromPath ignores comments and blank lines" {
            val tempDir = createTempDirectory("microsmith-plugin-allowlist-load")
            try {
                val allowlistPath = tempDir.resolve("plugins.allowlist")
                allowlistPath.writeText(
                    """
                    # comment

                    remote|com.acme:plugin:1.0.0|${"a".repeat(64)}
                    local|plugins/custom.jar|${"b".repeat(64)}
                    """.trimIndent(),
                )

                val allowlist = loadPluginChecksumAllowlistFromPath(allowlistPath)
                allowlist.entries shouldBe
                    mapOf(
                        LockKey(kind = REMOTE_KIND, key = "com.acme:plugin:1.0.0") to "a".repeat(64),
                        LockKey(kind = LOCAL_KIND, key = "plugins/custom.jar") to "b".repeat(64),
                    )
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "assertCovers reports sorted missing keys" {
            val allowlist = PluginChecksumAllowlist(
                entries = mapOf(LockKey(kind = REMOTE_KIND, key = "com.acme:plugin:1.0.0") to "a".repeat(64)),
            )

            val error =
                shouldThrow<IllegalArgumentException> {
                    allowlist.assertCovers(
                        setOf(
                            LockKey(kind = REMOTE_KIND, key = "com.acme:plugin:1.0.0"),
                            LockKey(kind = LOCAL_KIND, key = "plugins/custom.jar"),
                            LockKey(kind = REMOTE_ARTIFACT_KIND, key = "deps/shared.jar"),
                        ),
                    )
                }

            error.message.shouldContain("local:plugins/custom.jar")
            error.message.shouldContain("remote-artifact:deps/shared.jar")
        }
    })
