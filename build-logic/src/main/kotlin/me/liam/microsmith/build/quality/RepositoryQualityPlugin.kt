package me.liam.microsmith.build.quality

import org.gradle.api.Plugin
import org.gradle.api.Project

class RepositoryQualityPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) {
            "The me.liam.microsmith.repository-quality plugin must be applied to the root project."
        }

        project.pluginManager.apply("base")

        val verifyTask = project.tasks.register("verifyRepositoryStandards", RepositoryQualityTask::class.java) {
            repositoryRoot.set(project.layout.projectDirectory)
            productionSources.from(
                project.fileTree(project.layout.projectDirectory) {
                    include("**/src/main/kotlin/**/*.kt")
                    exclude("**/build/**")
                },
            )
        }

        project.tasks.named("check").configure {
            dependsOn(verifyTask)
        }
    }
}
