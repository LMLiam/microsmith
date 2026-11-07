package io.github.lmliam.microsmith.build.cli

import java.io.File
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

    init {
        group = "verification"
        description = "Verifies CLI distribution layout and archive package contents."
    }

    @TaskAction
    fun verify() {
        val distDir = distDirectory.asFile.get()
        val sourceCatalog = catalogFile.asFile.get()
        val zipFile = zipArchive.asFile.get()
        val tarFile = tarArchive.asFile.get()

        val distRoot = distRootName.get()
        val catalogName = catalogFileName.get()
        val readmeName = readmeFileName.get()
        val shadowJarName = shadowJarArchiveName.get()
        val unixLauncher = unixLauncherName.get()
        val windowsLauncher = windowsLauncherName.get()

        distDir.verifyCatalogCopy(catalogName, sourceCatalog)
        distDir.verifyLauncher(unixLauncher, shadowJarName)
        distDir.verifyLauncher(windowsLauncher, shadowJarName)

        val expectedEntries =
            setOf(
                "$distRoot/$catalogName",
                "$distRoot/$readmeName",
                "$distRoot/bin/$unixLauncher",
                "$distRoot/bin/$windowsLauncher",
                "$distRoot/lib/$shadowJarName",
            )

        verifyArchiveEntries(
            "ZIP",
            zipFile,
            CliBundledPluginCatalogVerifier.collectZipEntries(zipFile),
            expectedEntries,
        )
        verifyArchiveEntries(
            "TAR.GZ",
            tarFile,
            CliBundledPluginCatalogVerifier.collectTarGzEntries(tarFile),
            expectedEntries,
        )

        verifyCliArchiveLauncherPermissions(
            zipArchive = zipFile,
            tarArchive = tarFile,
            launcherEntry = "$distRoot/bin/$unixLauncher",
        )
    }

    private fun File.verifyCatalogCopy(
        catalogName: String,
        sourceCatalog: File,
    ) {
        val distCatalog = resolve(catalogName)
        if (!distCatalog.isFile) {
            throw GradleException("Distribution directory is missing '$catalogName' at '${distCatalog.path}'.")
        }

        if (distCatalog.readText() != sourceCatalog.readText()) {
            throw GradleException(
                "Distribution catalog '${distCatalog.path}' does not match generated bundled plugin metadata.",
            )
        }
    }

    private fun File.verifyLauncher(
        launcherName: String,
        shadowJarName: String,
    ) {
        val launcher = resolve("bin/$launcherName")
        if (!launcher.isFile) {
            throw GradleException("Distribution launcher is missing at '${launcher.path}'.")
        }

        val content = launcher.readText()
        if ("@CLI_JAR@" in content) {
            throw GradleException("Launcher '${launcher.path}' still contains unresolved @CLI_JAR@ token.")
        }
        if (shadowJarName !in content) {
            throw GradleException("Launcher '${launcher.path}' does not reference '$shadowJarName'.")
        }
    }

    private fun verifyArchiveEntries(
        formatLabel: String,
        archiveFile: File,
        actualEntries: Set<String>,
        expectedEntries: Set<String>,
    ) {
        val missingEntries = expectedEntries.filterNot(actualEntries::contains)
        if (missingEntries.isNotEmpty()) {
            val formattedMissing = missingEntries.joinToString(", ")
            throw GradleException(
                "Archive $formatLabel '${archiveFile.name}' is missing expected distribution entries: $formattedMissing",
            )
        }
    }
}
