package me.liam.microsmith.runtime.scripting.cache

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class MicrosmithScriptCacheTests :
    StringSpec({
        "classpath fingerprint changes when classpath file content changes" {
            val tempDir = createTempDirectory("microsmith-classpath-fingerprint")
            try {
                val classpathEntry = tempDir.resolve("cli.jar")
                classpathEntry.writeText("abc")

                val firstFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))

                classpathEntry.writeText("xyz")
                val secondFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))

                secondFingerprint shouldNotBe firstFingerprint
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "classpath fingerprint is stable when classpath file content is unchanged" {
            val tempDir = createTempDirectory("microsmith-classpath-fingerprint-stable")
            try {
                val classpathEntry = tempDir.resolve("cli.jar")
                classpathEntry.writeText("same-content")

                val firstFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))
                val secondFingerprint = RuntimeClasspathFingerprint.calculate(listOf(classpathEntry))

                secondFingerprint shouldBe firstFingerprint
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
