package me.liam.microsmith.cli.ide

import java.nio.file.Path

internal object IdeHelperManagedSurface {
    fun renderedFiles(helperRoot: Path, classpathEntries: List<Path>): Map<Path, String> = linkedMapOf(
        helperRoot.resolve(IDE_HELPER_SETTINGS_FILE_NAME) to IdeHelperProjectTemplates.renderSettingsGradle(),
        helperRoot.resolve(IDE_HELPER_BUILD_FILE_NAME) to
            IdeHelperProjectTemplates.renderBuildGradle(classpathEntries),
        helperRoot.resolve(IDE_HELPER_README_FILE_NAME) to IdeHelperProjectTemplates.renderReadme(),
    )

    fun requiredFiles(helperRoot: Path): List<Path> = listOf(
        helperRoot.resolve(IDE_HELPER_SETTINGS_FILE_NAME),
        helperRoot.resolve(IDE_HELPER_BUILD_FILE_NAME),
        helperRoot.resolve(IDE_HELPER_README_FILE_NAME),
    )

    fun buildFile(helperRoot: Path): Path = helperRoot.resolve(IDE_HELPER_BUILD_FILE_NAME)
}
