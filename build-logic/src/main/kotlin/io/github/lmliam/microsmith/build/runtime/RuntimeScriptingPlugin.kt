package io.github.lmliam.microsmith.build.runtime

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.Sync
import org.gradle.jvm.tasks.Jar

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.jar.JarFile
import java.util.regex.Pattern

class RuntimeScriptingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("java-library")

        val scriptTemplateSourceFile =
            RuntimeScriptingSourceFiles.annotatedKotlinScriptSourceFile(project, "microsmith.kts")
        val scriptTemplateFqcn = RuntimeScriptingSourceFiles.fqcnFromSourceFile(scriptTemplateSourceFile)
        val scriptCompilationConfigurationSourceFile =
            RuntimeScriptingSourceFiles.sourceFileBySimpleName(
                project,
                RuntimeScriptingBuildNames.SCRIPT_DEFINITION_COMPILATION_CONFIGURATION_CLASS_NAME,
            )
        val scriptCompilationConfigurationFqcn =
            RuntimeScriptingSourceFiles.fqcnFromSourceFile(scriptCompilationConfigurationSourceFile)
        val scriptContextSourceFile =
            RuntimeScriptingSourceFiles.sourceFileBySimpleName(
                project,
                RuntimeScriptingBuildNames.SCRIPT_DEFINITION_CONTEXT_CLASS_NAME,
            )
        val scriptContextFqcn = RuntimeScriptingSourceFiles.fqcnFromSourceFile(scriptContextSourceFile)
        val scriptTemplateRegistrationJarEntry =
            RuntimeScriptingBuildNames.scriptTemplateRegistrationEntry(scriptTemplateFqcn)

        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

        project.dependencies.apply {
            RuntimeScriptingBuildNames.API_PROJECT_PATHS.forEach { add("api", project.project(it)) }
            RuntimeScriptingBuildNames.IMPLEMENTATION_PROJECT_PATHS.forEach { add("implementation", project.project(it)) }
            add("implementation", libs.findLibrary("coroutines-core").orElseThrow().get())
            add("api", libs.findLibrary("kotlin-scripting-common").orElseThrow().get())
            add("implementation", libs.findLibrary("kotlin-scripting-jvm").orElseThrow().get())
            add("implementation", libs.findLibrary("kotlin-scripting-jvm-host").orElseThrow().get())
        }

        val runtimeScriptingJarTask = project.tasks.named("jar", Jar::class.java)
        val runtimeScriptingJarArchive = runtimeScriptingJarTask.flatMap { it.archiveFile }
        val runtimeScriptingPomTask = project.tasks.named("generatePomFileForGprPublication")
        val runtimeScriptingPomFile = project.layout.buildDirectory.file(RuntimeScriptingBuildNames.POM_PATH)
        val runtimeScriptingJarExpectedEntries = listOf(
            RuntimeScriptingBuildNames.classEntryName(scriptTemplateFqcn),
            scriptTemplateRegistrationJarEntry,
        )
        val runtimeScriptingPomExpectedScopes = linkedMapOf<String, String>().apply {
            RuntimeScriptingBuildNames.API_PROJECT_PATHS.forEach { path ->
                val dependencyProject = project.project(path)
                put(RuntimeScriptingBuildNames.projectCoordinate(dependencyProject), "compile")
            }
            put(RuntimeScriptingBuildNames.libraryCoordinate(libs.findLibrary("kotlin-scripting-common").orElseThrow().get()), "compile")
        }
        val ideFallbackReleaseAssetsDirectory =
            project.layout.buildDirectory.dir(RuntimeScriptingBuildNames.RELATIVE_RELEASE_ASSETS_DIRECTORY)
        val ideFallbackShadowJarTask = project.tasks.named("shadowJar", Jar::class.java)
        val ideFallbackShadowJarArchive = ideFallbackShadowJarTask.flatMap { it.archiveFile }
        val ideFallbackExpectedEntries = listOf(
            RuntimeScriptingBuildNames.classEntryName(scriptTemplateFqcn),
            RuntimeScriptingBuildNames.classEntryName(scriptCompilationConfigurationFqcn),
            RuntimeScriptingBuildNames.classEntryName(scriptContextFqcn),
            "kotlin/script/experimental/jvmhost/BasicJvmScriptingHost.class",
            scriptTemplateRegistrationJarEntry,
        )

        ideFallbackShadowJarTask.configure { shadowJarTask ->
            shadowJarTask.archiveBaseName.set(RuntimeScriptingBuildNames.IDE_FALLBACK_SHADOW_JAR_BASE_NAME)
            shadowJarTask.archiveClassifier.set(RuntimeScriptingBuildNames.IDE_FALLBACK_SHADOW_JAR_CLASSIFIER)
            shadowJarTask.manifest.attributes(
                mapOf(
                    "Implementation-Version" to project.version.toString(),
                ),
            )
        }

        val verifyJarEntries = { archiveFile: java.io.File, expectedEntries: List<String>, artifactDescription: String ->
            if (!archiveFile.isFile) {
                throw GradleException("$artifactDescription '${archiveFile.path}' was not created.")
            }

            JarFile(archiveFile).use { jarFile ->
                val entryNames = jarFile.entries().asSequence().map { entry -> entry.name }.toSet()
                val missingEntries = expectedEntries.filterNot(entryNames::contains)
                if (missingEntries.isNotEmpty()) {
                    throw GradleException(
                        "$artifactDescription '${archiveFile.name}' is missing expected entries: ${missingEntries.joinToString(", ")}",
                    )
                }
            }
        }

        project.tasks.register("verifyRuntimeScriptingJar", DefaultTask::class.java) { verifyTask ->
            verifyTask.dependsOn(runtimeScriptingJarTask)
            verifyTask.inputs.file(runtimeScriptingJarArchive)

            verifyTask.doLast {
                verifyJarEntries(
                    runtimeScriptingJarArchive.get().asFile,
                    runtimeScriptingJarExpectedEntries,
                    "Runtime-scripting jar",
                )
            }
        }

        project.tasks.register("verifyRuntimeScriptingPublishedPom", DefaultTask::class.java) { verifyPomTask ->
            verifyPomTask.dependsOn(runtimeScriptingPomTask)
            verifyPomTask.inputs.file(runtimeScriptingPomFile)

            verifyPomTask.doLast {
                val pomFile = runtimeScriptingPomFile.get().asFile
                if (!pomFile.isFile) {
                    throw GradleException("Runtime-scripting published pom '${pomFile.path}' was not created.")
                }

                val pomContents = pomFile.readText(StandardCharsets.UTF_8)

                val invalidScopes = runtimeScriptingPomExpectedScopes.filterNot { (dependencyCoordinates, expectedScope) ->
                    val (groupId, artifactId) = dependencyCoordinates.split(":")
                    val dependencyPattern = Pattern.compile(
                        "(?s)<dependency>\\s*<groupId>${Pattern.quote(groupId)}</groupId>\\s*" +
                            "<artifactId>${Pattern.quote(artifactId)}</artifactId>\\s*" +
                            "<version>[^<]+</version>\\s*" +
                            "<scope>${Pattern.quote(expectedScope)}</scope>\\s*</dependency>",
                    )
                    dependencyPattern.matcher(pomContents).find()
                }

                if (invalidScopes.isNotEmpty()) {
                    val formattedInvalidScopes = invalidScopes.map { (dependencyCoordinates, expectedScope) ->
                        "$dependencyCoordinates expected $expectedScope scope"
                    }
                    throw GradleException(
                        "Runtime-scripting published pom '${pomFile.name}' had unexpected dependency scopes: ${formattedInvalidScopes.joinToString(", ")}",
                    )
                }
            }
        }

        project.tasks.register("verifyIdeFallbackShadowJar", DefaultTask::class.java) { verifyShadowTask ->
            verifyShadowTask.dependsOn(ideFallbackShadowJarTask)
            verifyShadowTask.inputs.file(ideFallbackShadowJarArchive)

            verifyShadowTask.doLast {
                verifyJarEntries(
                    ideFallbackShadowJarArchive.get().asFile,
                    ideFallbackExpectedEntries,
                    "IDE fallback shadow jar",
                )
            }
        }

        project.tasks.register("ideFallbackArtifacts", Sync::class.java) { syncTask ->
            syncTask.dependsOn(project.tasks.named("verifyIdeFallbackShadowJar"))
            syncTask.into(ideFallbackReleaseAssetsDirectory)
            syncTask.from(ideFallbackShadowJarArchive)
        }

        project.tasks.register("generateIdeFallbackChecksums", DefaultTask::class.java) { checksumTask ->
            checksumTask.dependsOn(project.tasks.named("ideFallbackArtifacts"))
            checksumTask.inputs.dir(ideFallbackReleaseAssetsDirectory)
            checksumTask.outputs.dir(ideFallbackReleaseAssetsDirectory)

            checksumTask.doLast {
                val releaseDir = ideFallbackReleaseAssetsDirectory.get().asFile
                if (!releaseDir.isDirectory) {
                    throw GradleException("IDE fallback release assets directory '${releaseDir.path}' does not exist.")
                }

                val sourceFiles =
                    releaseDir.listFiles()
                        ?.filter { file -> file.isFile && !file.name.endsWith(".sha256") }
                        ?.sortedBy { file -> file.name }
                        .orEmpty()
                if (sourceFiles.isEmpty()) {
                    throw GradleException(
                        "IDE fallback release assets directory '${releaseDir.path}' did not contain files to checksum.",
                    )
                }

                sourceFiles.forEach { file ->
                    val checksum = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).encodeHex()
                    java.io.File(releaseDir, "${file.name}.sha256").writeText("$checksum  ${file.name}\n", StandardCharsets.UTF_8)
                }
            }
        }

        project.tasks.named("check").configure { checkTask ->
            checkTask.dependsOn(project.tasks.named("verifyRuntimeScriptingJar"))
            checkTask.dependsOn(project.tasks.named("verifyRuntimeScriptingPublishedPom"))
            checkTask.dependsOn(project.tasks.named("verifyIdeFallbackShadowJar"))
        }
    }
}

private fun ByteArray.encodeHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
