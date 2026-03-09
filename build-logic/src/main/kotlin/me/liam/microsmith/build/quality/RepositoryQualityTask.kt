package me.liam.microsmith.build.quality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Repository structural checks are fast and scan the working tree directly.")
abstract class RepositoryQualityTask : DefaultTask() {
    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val productionSources: ConfigurableFileCollection

    init {
        group = "verification"
        description = "Verifies repository structural Kotlin quality guardrails."
    }

    @TaskAction
    fun verify() {
        val policy = RepositoryQualityPolicy.default()
        val sourceFiles = productionSources.files.map { file -> file.toPath() }
        val violations = RepositoryQualityValidator(policy).validate(repositoryRoot.asFile.get().toPath(), sourceFiles)
        if (violations.isEmpty()) {
            logger.lifecycle("Repository structural Kotlin quality guardrails passed.")
            return
        }

        val report = buildString {
            appendLine("Repository structural Kotlin quality guardrails failed:")
            violations.forEach { violation ->
                appendLine("- [${violation.rule}] ${violation.path}: ${violation.message}")
            }
            appendLine()
            appendLine(
                @Suppress("ktlint:standard:max-line-length")
                "Fix the structural issue, or if the exception is truly justified, update RepositoryQualityPolicy with a narrow path-specific rationale and mention it in the PR description.",
            )
            appendLine("Broader architecture and layering decisions remain review-gated in README.md.")
        }
        throw GradleException(report)
    }
}
