package io.github.lmliam.microsmith.build.formatting

import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinSourceFormattingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "The io.github.lmliam.microsmith.kotlin-source-formatting plugin must be applied to the root project."
        }

        project.allprojects
            .filter { module -> module != project }
            .forEach { module ->
                val kotlinSources = module.fileTree(module.layout.projectDirectory).apply {
                    include("src/**/*.kt")
                    include("src/**/*.kts")
                    exclude("**/build/**")
                    exclude("**/generated/**")
                }

                val formatSpacingTask = module.tasks.register(
                    "formatKotlinSourceSpacing",
                    FormatKotlinSourceSpacingTask::class.java,
                ) { task ->
                    task.sourceFiles.from(kotlinSources)
                }

                module.tasks.matching { task -> task.name == KTLINT_FORMAT_TASK }.configureEach { task ->
                    task.finalizedBy(formatSpacingTask)
                }
            }
    }

    private companion object {
        private const val KTLINT_FORMAT_TASK = "ktlintFormat"
    }
}
