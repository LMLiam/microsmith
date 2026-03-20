package io.github.lmliam.microsmith.build.maven

import groovy.util.Node
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.tasks.Jar

import javax.xml.parsers.DocumentBuilderFactory

class MavenPluginDescriptorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val pluginGroupId = project.group.toString()
        val pluginArtifactId = project.name
        val pluginVersion = project.version.toString()
        val pluginGoalPrefix = project.rootProject.name
        val descriptorFile = project.layout.buildDirectory.file(MavenPluginBuildNames.DESCRIPTOR_OUTPUT_PATH)
        val runtimeClasspath = project.configurations.getByName("runtimeClasspath")

        project.dependencies.apply {
            add("implementation", project.project(":runtime-scripting"))
            add("compileOnly", libs.findLibrary("maven-plugin-api").orElseThrow().get())
            add("testImplementation", libs.findLibrary("maven-plugin-api").orElseThrow().get())
        }

        val runtimeArtifacts = MavenPluginDescriptorWriter.resolveRuntimeArtifacts(runtimeClasspath)

        project.extensions.getByType(SourceSetContainer::class.java)
            .named("main")
            .configure { sourceSet ->
                sourceSet.resources.srcDir(project.layout.buildDirectory.dir(MavenPluginBuildNames.RESOURCE_DIR))
            }

        val generateMavenPluginDescriptorTask =
            project.tasks.register(MavenPluginBuildNames.DESCRIPTOR_TASK_NAME, DefaultTask::class.java) { task ->
                task.inputs.property("pluginGroupId", pluginGroupId)
                task.inputs.property("pluginArtifactId", pluginArtifactId)
                task.inputs.property("pluginVersion", pluginVersion)
                task.inputs.property("goalPrefix", pluginGoalPrefix)
                task.inputs.property("generateGoal", MavenPluginBuildNames.GENERATE_GOAL)
                task.inputs.files(runtimeClasspath)
                task.outputs.file(descriptorFile)

                task.doLast {
                    MavenPluginDescriptorWriter.write(
                        descriptorFile.get().asFile,
                        pluginGroupId,
                        pluginArtifactId,
                        pluginVersion,
                        pluginGoalPrefix,
                        MavenPluginBuildNames.GENERATE_GOAL,
                        runtimeArtifacts,
                    )
                }
            }

        project.tasks.named("processResources", Copy::class.java).configure { task ->
            task.dependsOn(generateMavenPluginDescriptorTask)
        }

        project.tasks.named("sourcesJar", Jar::class.java).configure { task ->
            task.dependsOn(generateMavenPluginDescriptorTask)
        }

        val verifyMavenPluginDescriptorTask =
            project.tasks.register(MavenPluginBuildNames.VERIFY_TASK_NAME, DefaultTask::class.java) { task ->
                task.dependsOn(generateMavenPluginDescriptorTask)
                task.inputs.file(descriptorFile)

                task.doLast {
                    val descriptorOutputFile = descriptorFile.get().asFile
                    if (!descriptorOutputFile.isFile) {
                        throw GradleException("Maven plugin descriptor '${descriptorOutputFile.path}' was not created.")
                    }

                    val descriptor = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(descriptorOutputFile)
                    val goalPrefix = descriptor.getElementsByTagName("goalPrefix").item(0)?.textContent?.trim()
                    if (goalPrefix != pluginGoalPrefix) {
                        throw GradleException(
                            "Expected Maven plugin goal prefix '${pluginGoalPrefix}', found '${goalPrefix}'.",
                        )
                    }

                    val goals =
                        (0 until descriptor.getElementsByTagName("goal").length)
                            .mapNotNull { index ->
                                descriptor.getElementsByTagName("goal").item(index)?.textContent?.trim()
                            }
                    if (goals != listOf(MavenPluginBuildNames.GENERATE_GOAL)) {
                        throw GradleException("Expected Maven plugin goals [${MavenPluginBuildNames.GENERATE_GOAL}], found ${goals}.")
                    }
                }
            }

        project.tasks.named("check").configure { task ->
            task.dependsOn(verifyMavenPluginDescriptorTask)
        }

        project.extensions.getByType(PublishingExtension::class.java).publications.named(
            "gpr",
            MavenPublication::class.java,
        ).configure { publication ->
            publication.artifactId = pluginArtifactId
            publication.pom.withXml { xmlProvider ->
                val root = xmlProvider.asNode()
                if (!root.children().any { child -> child is Node && child.name() == "packaging" }) {
                    root.appendNode("packaging", MavenPluginBuildNames.PACKAGING)
                }
            }
        }
    }
}
