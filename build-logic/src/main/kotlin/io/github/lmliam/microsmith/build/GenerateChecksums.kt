package io.github.lmliam.microsmith.build

import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Writes a SHA-256 sidecar next to each asset file.
 *
 * Each sidecar holds the checksum and the file name, in the format that `sha256sum -c`
 * and `shasum -a 256 -c` accept.
 */
abstract class GenerateChecksums : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val assets: ConfigurableFileCollection

    @TaskAction
    fun generate() {
        val files = assets.files.filter { file -> file.isFile }.sortedBy { file -> file.name }
        if (files.isEmpty()) {
            throw GradleException("No release assets were found to checksum.")
        }

        files.forEach { file ->
            val checksum = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).encodeHex()
            File(file.parentFile, "${file.name}.sha256").writeText(
                "$checksum  ${file.name}\n",
                StandardCharsets.UTF_8,
            )
        }
    }
}

private fun ByteArray.encodeHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
