package me.liam.microsmith.build.quality

import org.gradle.api.Plugin
import org.gradle.api.Project

class RepositoryQualityPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "The me.liam.microsmith.repository-quality plugin must be applied to the root project."
        }

        project.pluginManager.apply("base")

        val productionSources = project.fileTree(project.layout.projectDirectory).apply {
            include("**/src/main/kotlin/**/*.kt")
            exclude("**/build/**")
        }

        val verifyTask = project.tasks.register(
            "verifyRepositoryStandards",
            RepositoryQualityTask::class.java,
        ) { task ->
            task.repositoryRoot.set(project.layout.projectDirectory)
            task.productionSources.from(productionSources)
        }

        project.tasks.named("check").configure { task ->
            task.dependsOn(verifyTask)
        }
    }
}
