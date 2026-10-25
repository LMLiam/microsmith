package io.github.lmliam.microsmith.build.cli

import java.nio.charset.StandardCharsets
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateBundledPluginCatalog : CliBundledPluginTask() {
    @get:OutputFile
    abstract val catalogFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val plugins = bundledPlugins()
        val cliVersionValue = cliVersion.get()
        val formatVersionValue = formatVersion.get()
        val catalogText = CliBundledPluginProfile.catalogText(cliVersionValue, formatVersionValue, plugins)

        val outputFile = catalogFile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(catalogText, StandardCharsets.UTF_8)

        CliBundledPluginCatalogVerifier.validate(
            catalogText,
            outputFile.path,
            formatVersionValue,
            cliVersionValue,
            plugins.map { (coordinate) -> coordinate },
            CliBundledPluginProfile.serviceEntries(plugins),
        )
    }
}
