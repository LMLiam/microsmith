package io.github.lmliam.microsmith.build.cli

import java.io.File
import java.nio.charset.StandardCharsets
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails the build when the unpacked distribution or either archive deviates from the
 * release layout, from the generated catalog, or from the Unix launcher permission policy.
 */
abstract class VerifyDistLayout : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val distDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val catalogFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val zipArchive: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val tarArchive: RegularFileProperty

    @get:Input
    abstract val distRootName: Property<String>

    @get:Input
    abstract val catalogFileName: Property<String>

    @get:Input
    abstract val readmeFileName: Property<String>

    @get:Input
    abstract val shadowJarArchiveName: Property<String>

    @get:Input
    abstract val unixLauncherName: Property<String>

    @get:Input
    abstract val windowsLauncherName: Property<String>

    @TaskAction
    fun verify() {
        val distRoot = distRootName.get()
        val catalogName = catalogFileName.get()
        val unixLauncher = unixLauncherName.get()
        val windowsLauncher = windowsLauncherName.get()
        val shadowJarName = shadowJarArchiveName.get()
        val distRootDirectory = distDirectory.get().asFile

        verifyCatalogCopy(distRootDirectory, catalogName)

        listOf(unixLauncher, windowsLauncher).forEach { launcherName ->
            verifyLauncher(distRootDirectory, launcherName, shadowJarName)
        }

        val expectedEntries =
            setOf(
                "$distRoot/$catalogName",
                "$distRoot/${readmeFileName.get()}",
                "$distRoot/bin/$unixLauncher",
                "$distRoot/bin/$windowsLauncher",
                "$distRoot/lib/$shadowJarName",
            )

        mapOf(
            "zip" to CliBundledPluginCatalogVerifier.collectZipEntries(zipArchive.get().asFile),
            "tar.gz" to CliBundledPluginCatalogVerifier.collectTarGzEntries(tarArchive.get().asFile),
        ).forEach { (archiveName, entries) ->
            val missingEntries = expectedEntries.filterNot(entries::contains)
            if (missingEntries.isNotEmpty()) {
                throw GradleException(
                    "Archive $archiveName is missing expected distribution entries: " +
                        missingEntries.joinToString(", "),
                )
            }
        }

        verifyCliArchiveLauncherPermissions(
            zipArchive = zipArchive.get().asFile,
            tarArchive = tarArchive.get().asFile,
            launcherEntry = "$distRoot/bin/$unixLauncher",
        )
    }

    private fun verifyCatalogCopy(
        distRootDirectory: File,
        catalogName: String,
    ) {
        val distCatalogFile = File(distRootDirectory, catalogName)
        if (!distCatalogFile.isFile) {
            throw GradleException(
                "Distribution directory is missing '$catalogName' at ${distCatalogFile.path}.",
            )
        }

        val expectedCatalogText = catalogFile.get().asFile.readText(StandardCharsets.UTF_8)
        val distCatalogText = distCatalogFile.readText(StandardCharsets.UTF_8)
        if (distCatalogText != expectedCatalogText) {
            throw GradleException(
                "Distribution catalog '${distCatalogFile.path}' does not match generated bundled plugin metadata.",
            )
        }
    }

    private fun verifyLauncher(
        distRootDirectory: File,
        launcherName: String,
        shadowJarName: String,
    ) {
        val launcher = File(distRootDirectory, "bin/$launcherName")
        if (!launcher.isFile) {
            throw GradleException("Distribution launcher is missing at ${launcher.path}.")
        }

        val launcherText = launcher.readText(StandardCharsets.UTF_8)
        if (launcherText.contains("@CLI_JAR@")) {
            throw GradleException("Launcher '${launcher.path}' still contains unresolved @CLI_JAR@ token.")
        }
        if (!launcherText.contains(shadowJarName)) {
            throw GradleException("Launcher '${launcher.path}' does not reference '$shadowJarName'.")
        }
    }
}
