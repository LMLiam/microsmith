package io.github.lmliam.microsmith.build

import java.io.File
import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateChecksums : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val assets: ConfigurableFileCollection

    init {
        group = "build"
        description = "Generates SHA-256 checksum sidecar files for release assets."
    }

    @TaskAction
    fun generate() {
        val targetFiles = assets.files.filter { it.isFile }.sortedBy { it.name }
        if (targetFiles.isEmpty()) {
            throw GradleException("No release assets were found to checksum.")
        }

        for (file in targetFiles) {
            val checksum = file.sha256Hex()
            val sidecarFile = file.parentFile.resolve("${file.name}.sha256")
            sidecarFile.writeText("$checksum  ${file.name}\n")
        }
    }
}

private fun File.sha256Hex(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { stream ->
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (stream.read(buffer).also { bytesRead = it } != -1) {
            digest.update(buffer, 0, bytesRead)
        }
    }
    return digest.digest().toHexString()
}

private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }
