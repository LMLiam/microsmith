package io.github.lmliam.microsmith.build.cli

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFilePermissions
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.bundling.Compression
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.jar.JarFile

class CliPackagingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply("application")
        project.pluginManager.apply("com.gradleup.shadow")

        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        val application = project.extensions.getByType(JavaApplication::class.java)
        application.mainClass.set(CliSourceFiles.applicationMainClass(project))

        project.dependencies.apply {
            add("implementation", project.project(":runtime-scripting"))
            add("implementation", project.project(":dsl"))
            add("implementation", project.project(":dsl-schemas"))
            add("implementation", project.project(":dsl-schemas-protobuf"))
            add("implementation", project.project(":dsl-schemas-protobuf-rpc"))
            add("implementation", project.project(":resolve"))
            add("implementation", project.project(":resolve-schemas"))
            add("implementation", project.project(":resolve-schemas-protobuf"))
            add("implementation", project.project(":resolve-schemas-protobuf-rpc"))
            add("implementation", project.project(":artifact"))
            add("implementation", project.project(":artifact-schemas"))
            add("implementation", project.project(":artifact-schemas-protobuf"))
            add("implementation", project.project(":artifact-schemas-protobuf-rpc"))
            add("implementation", project.project(":compile"))
            add("implementation", project.project(":compile-schemas"))
            add("implementation", project.project(":compile-schemas-protobuf"))
            add("implementation", project.project(":compile-schemas-protobuf-rpc"))
            add("implementation", project.project(":gen"))
            add("implementation", libs.findLibrary("maven-resolver-api").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-spi").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-util").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-impl").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-supplier").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-connector-basic").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-transport-file").orElseThrow().get())
            add("implementation", libs.findLibrary("maven-resolver-transport-http").orElseThrow().get())
        }

        val bundledPluginCatalogDirectory = project.layout.buildDirectory.dir("generated/microsmith")
        val bundledPluginJarTasks =
            CliPackagingBuildNames.BUNDLED_PLUGIN_PROJECT_PATHS.map { path ->
                project.project(path).tasks.named("jar", Jar::class.java)
            }
        val bundledPlugins = CliPackagingBuildNames.BUNDLED_PLUGIN_PROJECT_PATHS.map { path ->
            val bundledProject = project.project(path)
            BundledPluginCatalogEntry(
                coordinate = CliPackagingBuildNames.bundledPluginCoordinate(bundledProject),
                archiveFile = bundledProject.tasks.named("jar", Jar::class.java).flatMap { it.archiveFile },
            )
        }
        val bundledPluginCoordinates = bundledPlugins.map { it.coordinate }
        fun collectBundledServiceEntries(): List<BundledServiceEntry> =
            bundledPlugins.flatMap { it.collectServiceEntries() }

        val cliVersion = project.version.toString()
        val bundledPluginCatalogOutput =
            project.layout.buildDirectory.file(
                "generated/microsmith/${CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FILE_NAME}",
            )
        val bundledPluginCatalogJarEntry = CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_JAR_PATH
        val shadowJarTask = project.tasks.named("shadowJar", ShadowJar::class.java)
        val shadowJarArchive = shadowJarTask.flatMap { it.archiveFile }
        val shadowJarArchiveName = CliPackagingBuildNames.shadowJarArchiveName(cliVersion)
        val distRootName = CliPackagingBuildNames.distRootName(cliVersion)
        val distDirectory = project.layout.buildDirectory.dir(CliPackagingBuildNames.DIST_BUILD_DIRECTORY)
        val releaseAssetsDirectory = project.layout.buildDirectory.dir(CliPackagingBuildNames.RELEASE_ASSETS_DIRECTORY)

        project.extensions.getByType(SourceSetContainer::class.java)
            .named("main")
            .configure { sourceSet -> sourceSet.resources.srcDir(bundledPluginCatalogDirectory) }

        val generateBundledPluginCatalogTask =
            project.tasks.register(CliTaskNames.GENERATE_BUNDLED_PLUGIN_CATALOG, DefaultTask::class.java) { task ->
                task.dependsOn(bundledPluginJarTasks)
                task.inputs.property("cliVersion", cliVersion)
                task.inputs.property(
                    "catalogFormatVersion",
                    CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION,
                )
                task.inputs.files(bundledPlugins.map { it.archiveFile })
                task.outputs.file(bundledPluginCatalogOutput)

                task.doLast {
                    val outputFile = bundledPluginCatalogOutput.get().asFile
                    outputFile.parentFile.mkdirs()
                    val bundledServiceEntries = collectBundledServiceEntries()
                    val catalogText =
                        CliBundledPluginCatalogWriter.buildText(
                            cliVersion = cliVersion,
                            formatVersion = CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION,
                            bundledPlugins = bundledPlugins,
                            bundledServiceEntries = bundledServiceEntries,
                        )
                    outputFile.writeText(catalogText, StandardCharsets.UTF_8)
                    CliBundledPluginCatalogVerifier.validate(
                        catalogText,
                        outputFile.path,
                        CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION,
                        cliVersion,
                        bundledPluginCoordinates,
                        bundledServiceEntries,
                    )
                }
            }

        project.tasks.named("processResources", Copy::class.java).configure { task ->
            task.dependsOn(generateBundledPluginCatalogTask)
            task.from(
                bundledPluginCatalogOutput,
                Action { copySpec ->
                    copySpec.into("META-INF/microsmith")
                },
            )
        }

        project.tasks.named("sourcesJar", Jar::class.java).configure { task ->
            task.dependsOn(generateBundledPluginCatalogTask)
        }

        shadowJarTask.configure { shadowJar ->
            shadowJar.archiveBaseName.set(CliPackagingBuildNames.SHADOW_JAR_BASE_NAME)
            shadowJar.archiveClassifier.set(CliPackagingBuildNames.SHADOW_JAR_CLASSIFIER)
            shadowJar.filesMatching("META-INF/services/**") { fileCopyDetails ->
                fileCopyDetails.duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
            shadowJar.mergeServiceFiles()
            shadowJar.manifest.attributes(
                mapOf(
                    "Main-Class" to application.mainClass.get(),
                    "Implementation-Version" to cliVersion,
                ),
            )
            shadowJar.outputs.cacheIf { false }
        }

        val verifyShadowJarServices =
            project.tasks.register(CliTaskNames.VERIFY_SHADOW_JAR_SERVICES, DefaultTask::class.java) { task ->
                task.dependsOn(shadowJarTask, generateBundledPluginCatalogTask)
                task.inputs.file(shadowJarArchive)
                task.inputs.file(bundledPluginCatalogOutput)

                task.doLast {
                    val shadedJar = shadowJarArchive.get().asFile
                    val expectedCatalogText = bundledPluginCatalogOutput.get().asFile.readText(StandardCharsets.UTF_8)
                    val bundledServiceEntries = collectBundledServiceEntries()
                    val expectedServiceProvidersByDescriptor =
                        bundledServiceEntries.groupBy { it.servicePath }.mapValues { (_, entries) ->
                            entries.map { it.provider }
                        }
                    CliBundledPluginCatalogVerifier.validate(
                        expectedCatalogText,
                        bundledPluginCatalogOutput.get().asFile.path,
                        CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION,
                        cliVersion,
                        bundledPluginCoordinates,
                        bundledServiceEntries,
                    )
                    JarFile(shadedJar).use { jarFile ->
                        expectedServiceProvidersByDescriptor.forEach { (servicePath, expectedProviders) ->
                            val entry = jarFile.getJarEntry(servicePath)
                                ?: throw GradleException(
                                    "Missing merged service descriptor '$servicePath' in ${shadedJar.name}.",
                                )
                            val providers =
                                jarFile.getInputStream(entry)
                                    .bufferedReader(StandardCharsets.UTF_8)
                                    .use { reader ->
                                        reader.lineSequence()
                                            .map(String::trim)
                                            .filter { line -> line.isNotEmpty() && !line.startsWith("#") }
                                            .toList()
                                    }
                            expectedProviders.forEach { expectedProvider ->
                                if (expectedProvider !in providers) {
                                    throw GradleException(
                                        "Service '$servicePath' is missing expected provider '$expectedProvider' in ${shadedJar.name}.",
                                    )
                                }
                            }
                        }

                        val catalogEntry = jarFile.getJarEntry(bundledPluginCatalogJarEntry)
                            ?: throw GradleException(
                                "Missing bundled plugin catalog '$bundledPluginCatalogJarEntry' in ${shadedJar.name}.",
                            )
                        val actualCatalogText =
                            jarFile.getInputStream(catalogEntry)
                                .bufferedReader(StandardCharsets.UTF_8)
                                .use { reader -> reader.readText() }
                        if (actualCatalogText != expectedCatalogText) {
                            throw GradleException(
                                "Bundled plugin catalog in ${shadedJar.name} does not match generated metadata.",
                            )
                        }
                        CliBundledPluginCatalogVerifier.validate(
                            actualCatalogText,
                            "${shadedJar.name}!/$bundledPluginCatalogJarEntry",
                            CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION,
                            cliVersion,
                            bundledPluginCoordinates,
                            bundledServiceEntries,
                        )
                    }
                }
            }

        project.tasks.named("check").configure { task ->
            task.dependsOn(verifyShadowJarServices)
        }

        val prepareDistTask =
            project.tasks.register(CliTaskNames.PREPARE_DIST, Sync::class.java) { task ->
                task.dependsOn(shadowJarTask, generateBundledPluginCatalogTask)
                task.into(distDirectory)
                task.from(
                    shadowJarArchive,
                    Action { copySpec ->
                        copySpec.into("lib")
                    },
                )
                task.from(
                    project.file("src/dist/launcher/microsmith"),
                    launcherCopySpec(shadowJarArchiveName),
                )
                task.from(
                    project.file("src/dist/launcher/microsmith.bat"),
                    windowsLauncherCopySpec(shadowJarArchiveName),
                )
                task.from(
                    project.file("src/dist/README.txt"),
                    Action { copySpec ->
                        copySpec.into(".")
                    },
                )
                task.from(
                    bundledPluginCatalogOutput,
                    Action { copySpec ->
                        copySpec.into(".")
                    },
                )
            }

        val cliDistZipTask =
            project.tasks.register(CliTaskNames.CLI_DIST_ZIP, Zip::class.java) { task ->
                task.dependsOn(prepareDistTask)
                task.archiveBaseName.set(CliPackagingBuildNames.SHADOW_JAR_BASE_NAME)
                task.archiveVersion.set(cliVersion)
                task.archiveClassifier.set("dist")
                task.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
                task.from(
                    distDirectory,
                    Action { copySpec ->
                        copySpec.into(distRootName)
                    },
                )
            }
        val cliDistTarTask =
            project.tasks.register(CliTaskNames.CLI_DIST_TAR, Tar::class.java) { task ->
                task.dependsOn(prepareDistTask)
                task.compression = Compression.GZIP
                task.archiveBaseName.set(CliPackagingBuildNames.SHADOW_JAR_BASE_NAME)
                task.archiveVersion.set(cliVersion)
                task.archiveClassifier.set("dist")
                task.archiveExtension.set("tar.gz")
                task.destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
                task.from(
                    distDirectory,
                    Action { copySpec ->
                        copySpec.into(distRootName)
                    },
                )
            }

        val cliDistZipArchive = cliDistZipTask.flatMap { it.archiveFile }
        val cliDistTarArchive = cliDistTarTask.flatMap { it.archiveFile }

        val verifyDistLayoutTask =
            project.tasks.register(CliTaskNames.VERIFY_DIST_LAYOUT, DefaultTask::class.java) { task ->
                task.dependsOn(prepareDistTask, cliDistZipTask, cliDistTarTask, generateBundledPluginCatalogTask)
                task.inputs.dir(distDirectory)
                task.inputs.file(bundledPluginCatalogOutput)
                task.inputs.file(cliDistZipArchive)
                task.inputs.file(cliDistTarArchive)

                task.doLast {
                    val expectedCatalogText = bundledPluginCatalogOutput.get().asFile.readText(StandardCharsets.UTF_8)
                    val bundledServiceEntries = collectBundledServiceEntries()
                    val distRootDirectory = distDirectory.get().asFile
                    val distCatalogFile = java.io.File(
                        distRootDirectory,
                        CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FILE_NAME,
                    )
                    if (!distCatalogFile.isFile) {
                        throw GradleException(
                            "Distribution directory is missing '${CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FILE_NAME}' at ${distCatalogFile.path}.",
                        )
                    }
                    val distCatalogText = distCatalogFile.readText(StandardCharsets.UTF_8)
                    if (distCatalogText != expectedCatalogText) {
                        throw GradleException(
                            "Distribution catalog '${distCatalogFile.path}' does not match generated bundled plugin metadata.",
                        )
                    }
                    CliBundledPluginCatalogVerifier.validate(
                        distCatalogText,
                        distCatalogFile.path,
                        CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FORMAT_VERSION,
                        cliVersion,
                        bundledPluginCoordinates,
                        bundledServiceEntries,
                    )

                    listOf(
                        java.io.File(distRootDirectory, "bin/microsmith"),
                        java.io.File(distRootDirectory, "bin/microsmith.bat"),
                    ).forEach { launcher ->
                        if (!launcher.isFile) {
                            throw GradleException("Distribution launcher is missing at ${launcher.path}.")
                        }
                        val launcherText = launcher.readText(StandardCharsets.UTF_8)
                        if (launcherText.contains("@CLI_JAR@")) {
                            throw GradleException(
                                "Launcher '${launcher.path}' still contains unresolved @CLI_JAR@ token.",
                            )
                        }
                        if (!launcherText.contains(shadowJarArchiveName)) {
                            throw GradleException(
                                "Launcher '${launcher.path}' does not reference '$shadowJarArchiveName'.",
                            )
                        }
                    }

                    val expectedEntries =
                        setOf(
                            "$distRootName/${CliPackagingBuildNames.BUNDLED_PLUGIN_CATALOG_FILE_NAME}",
                            "$distRootName/README.txt",
                            "$distRootName/bin/microsmith",
                            "$distRootName/bin/microsmith.bat",
                            "$distRootName/lib/$shadowJarArchiveName",
                        )

                    mapOf(
                        "zip" to CliBundledPluginCatalogVerifier.collectZipEntries(cliDistZipArchive.get().asFile),
                        "tar.gz" to CliBundledPluginCatalogVerifier.collectTarGzEntries(cliDistTarArchive.get().asFile),
                    ).forEach { (archiveName, entries) ->
                        val missingEntries = expectedEntries.filterNot(entries::contains)
                        if (missingEntries.isNotEmpty()) {
                            throw GradleException(
                                "Archive $archiveName is missing expected distribution entries: ${missingEntries.joinToString(
                                    ", ",
                                )}",
                            )
                        }
                    }
                }
            }

        val distArtifactsTask =
            project.tasks.register(CliTaskNames.DIST_ARTIFACTS, Sync::class.java) { task ->
                task.dependsOn(verifyShadowJarServices, verifyDistLayoutTask)
                task.into(releaseAssetsDirectory)
                task.from(shadowJarArchive)
                task.from(cliDistZipTask)
                task.from(cliDistTarTask)
                task.from(
                    project.file("src/install/microsmith-install.sh"),
                    executableFileCopySpec(),
                )
                task.from(
                    project.file("src/install/microsmith-install.ps1"),
                    Action { copySpec ->
                        copySpec.into(".")
                    },
                )
            }

        project.tasks.register(CliTaskNames.GENERATE_RELEASE_CHECKSUMS, DefaultTask::class.java) { task ->
            task.dependsOn(distArtifactsTask)
            task.inputs.dir(releaseAssetsDirectory)
            task.outputs.dir(releaseAssetsDirectory)

            task.doLast {
                val releaseDir = releaseAssetsDirectory.get().asFile
                if (!releaseDir.isDirectory) {
                    throw GradleException("Release assets directory '${releaseDir.path}' does not exist.")
                }

                val sourceFiles =
                    releaseDir.listFiles()
                        ?.filter { file -> file.isFile && !file.name.endsWith(".sha256") }
                        ?.sortedBy { file -> file.name }
                        .orEmpty()
                if (sourceFiles.isEmpty()) {
                    throw GradleException(
                        "Release assets directory '${releaseDir.path}' did not contain files to checksum.",
                    )
                }

                sourceFiles.forEach { file ->
                    val checksum = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).encodeHex()
                    java.io.File(
                        releaseDir,
                        "${file.name}.sha256",
                    ).writeText("$checksum  ${file.name}\n", StandardCharsets.UTF_8)
                }
            }
        }

        project.tasks.register(CliTaskNames.RELEASE_ARTIFACTS, DefaultTask::class.java) { task ->
            task.dependsOn(distArtifactsTask, project.tasks.named(CliTaskNames.GENERATE_RELEASE_CHECKSUMS))
        }
    }
}

private object CliTaskNames {
    const val GENERATE_BUNDLED_PLUGIN_CATALOG = "generateBundledPluginCatalog"
    const val VERIFY_SHADOW_JAR_SERVICES = "verifyShadowJarServices"
    const val PREPARE_DIST = "prepareDist"
    const val CLI_DIST_ZIP = "cliDistZip"
    const val CLI_DIST_TAR = "cliDistTar"
    const val VERIFY_DIST_LAYOUT = "verifyDistLayout"
    const val DIST_ARTIFACTS = "distArtifacts"
    const val GENERATE_RELEASE_CHECKSUMS = "generateReleaseChecksums"
    const val RELEASE_ARTIFACTS = "releaseArtifacts"
}

private fun launcherCopySpec(shadowJarArchiveName: String): Action<CopySpec> = Action { copySpec ->
    copySpec.into("bin")
    copySpec.filter(
        mapOf("tokens" to mapOf("CLI_JAR" to shadowJarArchiveName)),
        ReplaceTokens::class.java,
    )
    copySpec.filePermissions(
        Action { permissions ->
            permissions.unix("rwxr-xr-x")
        },
    )
}

private fun windowsLauncherCopySpec(shadowJarArchiveName: String): Action<CopySpec> = Action { copySpec ->
    copySpec.into("bin")
    copySpec.filter(
        mapOf("tokens" to mapOf("CLI_JAR" to shadowJarArchiveName)),
        ReplaceTokens::class.java,
    )
}

private fun executableFileCopySpec(): Action<CopySpec> = Action { copySpec ->
    copySpec.into(".")
    copySpec.filePermissions(
        Action { permissions ->
            permissions.unix("rwxr-xr-x")
        },
    )
}

private fun ByteArray.encodeHex(): String = joinToString(separator = "") { byte -> "%02x".format(byte) }
