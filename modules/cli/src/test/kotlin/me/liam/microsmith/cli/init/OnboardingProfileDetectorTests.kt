package me.liam.microsmith.cli.init

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class OnboardingProfileDetectorTests :
    StringSpec({
        "keeps a specific profile when multiple matchers map to the same profile" {
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("pnpm-workspace.yaml") },
                    ),
                )

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = NodeOnboardingProfile,
                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                    matchedMarkers = listOf("package.json", "pnpm-workspace.yaml"),
                )
        }

        "deduplicates repeated markers from overlapping matchers" {
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                    ),
                )

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = NodeOnboardingProfile,
                    selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                    matchedMarkers = listOf("package.json"),
                )
        }

        "falls back to the generic profile when multiple distinct profiles match" {
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(GoOnboardingProfile) { listOf("go.mod") },
                    ),
                )

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = GenericOnboardingProfile,
                    selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                    matchedMarkers = listOf("go.mod", "package.json"),
                )
        }

        "uses the configured fallback profile when no markers match" {
            val customFallback =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("custom-fallback")
                    override val displayName: String = "Custom"
                    override val sampleMessageName: String = "CustomUserCreated"
                    override val recommendedOutputDirectory: String? = null
                }
            val detector = OnboardingProfileDetector(matchers = emptyList(), fallbackProfile = customFallback)

            detector.detect(UNUSED_PROJECT_ROOT) shouldBe
                OnboardingProfileDetection(
                    profile = customFallback,
                    selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                    matchedMarkers = emptyList(),
                )
        }

        "formatting uses selection reason rather than generic profile identity" {
            val customFallback =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("custom-fallback")
                    override val displayName: String = "Custom"
                    override val sampleMessageName: String = "CustomUserCreated"
                    override val recommendedOutputDirectory: String? = null
                }
            val detector =
                OnboardingProfileDetector(
                    matchers =
                    listOf(
                        OnboardingProfileMatcher(NodeOnboardingProfile) { listOf("package.json") },
                        OnboardingProfileMatcher(GoOnboardingProfile) { listOf("go.mod") },
                    ),
                    fallbackProfile = customFallback,
                )

            val detection = detector.detect(UNUSED_PROJECT_ROOT)

            detection shouldBe
                OnboardingProfileDetection(
                    profile = customFallback,
                    selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                    matchedMarkers = listOf("go.mod", "package.json"),
                )
            detection.describeForComment().shouldContain("multiple repository markers matched")
            detection.describeForSummary().shouldContain("multiple markers matched")
        }

        "rejects conflicting profile definitions that reuse the same stable id" {
            val primaryProfile =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python"
                    override val sampleMessageName: String = "PythonUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }
            val conflictingProfile =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python Variant"
                    override val sampleMessageName: String = "PythonVariantUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }

            val error =
                io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                    OnboardingProfileDetector(
                        matchers =
                        listOf(
                            OnboardingProfileMatcher(primaryProfile) { listOf("pyproject.toml") },
                            OnboardingProfileMatcher(conflictingProfile) { listOf("requirements.txt") },
                        ),
                    )
                }

            error.message.shouldContain("python")
        }

        "rejects fallback profile definitions that reuse a matcher stable id" {
            val primaryProfile =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python"
                    override val sampleMessageName: String = "PythonUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }
            val conflictingFallback =
                object : OnboardingProfile {
                    override val id: OnboardingProfileId = OnboardingProfileId("python")
                    override val displayName: String = "Python Fallback"
                    override val sampleMessageName: String = "PythonFallbackUserCreated"
                    override val recommendedOutputDirectory: String? = "./generated"
                }

            val error =
                io.kotest.assertions.throwables.shouldThrow<IllegalArgumentException> {
                    OnboardingProfileDetector(
                        matchers = listOf(OnboardingProfileMatcher(primaryProfile) { listOf("pyproject.toml") }),
                        fallbackProfile = conflictingFallback,
                    )
                }

            error.message.shouldContain("python")
        }

        "ignores .NET marker traversal failures while detecting other repository markers" {
            val nodeRoot = createTempDirectory("microsmith-init-detect-node-traversal-failure")
            try {
                nodeRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingProfile(
                    projectRoot = nodeRoot,
                    detector = detectorWithDotnetMarkerFinder { throw IOException("permission denied") },
                ) shouldBe
                    OnboardingProfileDetection(
                        profile = NodeOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("package.json"),
                    )
            } finally {
                runCatching { nodeRoot.deleteRecursively() }
            }
        }

        "ignores unchecked .NET marker traversal failures while detecting other repository markers" {
            val nodeRoot = createTempDirectory("microsmith-init-detect-node-unchecked-traversal-failure")
            try {
                nodeRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")

                detectOnboardingProfile(
                    projectRoot = nodeRoot,
                    detector = detectorWithDotnetMarkerFinder {
                        throw UncheckedIOException(IOException("permission denied"))
                    },
                ) shouldBe
                    OnboardingProfileDetection(
                        profile = NodeOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("package.json"),
                    )
            } finally {
                runCatching { nodeRoot.deleteRecursively() }
            }
        }

        ".NET detection skips unreadable directories".config(enabled = supportsPosixPermissions()) {
            val dotnetRoot = createTempDirectory("microsmith-init-detect-dotnet-unreadable-directory")
            val unreadableDirectory = dotnetRoot.resolve("restricted")
            val readableProject = dotnetRoot.resolve("src/apps/service/Fixture.csproj")
            val unreadableProject = unreadableDirectory.resolve("Ignored.csproj")
            readableProject.parent.createDirectories()
            unreadableDirectory.createDirectories()
            readableProject.writeText("<Project Sdk=\"Microsoft.NET.Sdk\" />\n")
            unreadableProject.writeText("<Project Sdk=\"Microsoft.NET.Sdk\" />\n")

            val originalPermissions = Files.getPosixFilePermissions(unreadableDirectory)
            Files.setPosixFilePermissions(unreadableDirectory, emptySet())
            try {
                detectOnboardingProfile(dotnetRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = DotnetOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("src/apps/service/Fixture.csproj"),
                    )
            } finally {
                runCatching {
                    Files.setPosixFilePermissions(unreadableDirectory, originalPermissions)
                }
                runCatching { dotnetRoot.deleteRecursively() }
            }
        }

        listOf("pyproject.toml", "requirements.txt", "setup.py", "setup.cfg").forEach { markerFileName ->
            "detects Python repositories from $markerFileName" {
                val pythonRoot = createTempDirectory("microsmith-init-detect-python")
                try {
                    pythonRoot.resolve(markerFileName).writeText("# marker\n")

                    detectOnboardingProfile(pythonRoot) shouldBe
                        OnboardingProfileDetection(
                            profile = PythonOnboardingProfile,
                            selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                            matchedMarkers = listOf(markerFileName),
                        )
                } finally {
                    runCatching { pythonRoot.deleteRecursively() }
                }
            }
        }

        "detects Kotlin repositories from source roots across Maven, Gradle, and build-tool-light layouts" {
            val mavenRoot = createTempDirectory("microsmith-init-detect-kotlin-maven")
            val gradleKotlinDslRoot = createTempDirectory("microsmith-init-detect-kotlin-gradle-kts")
            val gradleGroovyRoot = createTempDirectory("microsmith-init-detect-kotlin-gradle-groovy")
            val multiplatformRoot = createTempDirectory("microsmith-init-detect-kotlin-multiplatform")
            val lightweightRoot = createTempDirectory("microsmith-init-detect-kotlin-lightweight")
            try {
                mavenRoot.resolve("pom.xml").writeText("<project />\n")
                mavenRoot.resolve("src/main/kotlin/example").createDirectories()
                gradleKotlinDslRoot.resolve(
                    "build.gradle.kts",
                ).writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
                gradleKotlinDslRoot.resolve(
                    "settings.gradle.kts",
                ).writeText("rootProject.name = \"fixture-kotlin\"\n")
                gradleKotlinDslRoot.resolve("src/test/kotlin/example").createDirectories()
                gradleGroovyRoot.resolve(
                    "build.gradle",
                ).writeText("plugins { id 'org.jetbrains.kotlin.jvm' version '2.2.21' }\n")
                gradleGroovyRoot.resolve(
                    "settings.gradle",
                ).writeText("rootProject.name = 'fixture-kotlin'\n")
                gradleGroovyRoot.resolve("src/main/kotlin/example").createDirectories()
                multiplatformRoot.resolve(
                    "build.gradle.kts",
                ).writeText("plugins { kotlin(\"multiplatform\") version \"2.2.21\" }\n")
                multiplatformRoot.resolve("src/commonMain/kotlin/example").createDirectories()
                lightweightRoot.resolve("src/main/kotlin/example").createDirectories()

                detectOnboardingProfile(mavenRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pom.xml", "src/main/kotlin"),
                    )
                detectOnboardingProfile(gradleKotlinDslRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "settings.gradle.kts", "src/test/kotlin"),
                    )
                detectOnboardingProfile(gradleGroovyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle", "settings.gradle", "src/main/kotlin"),
                    )
                detectOnboardingProfile(multiplatformRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "src/commonMain/kotlin"),
                    )
                detectOnboardingProfile(lightweightRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("src/main/kotlin"),
                    )
            } finally {
                runCatching { mavenRoot.deleteRecursively() }
                runCatching { gradleKotlinDslRoot.deleteRecursively() }
                runCatching { gradleGroovyRoot.deleteRecursively() }
                runCatching { multiplatformRoot.deleteRecursively() }
                runCatching { lightweightRoot.deleteRecursively() }
            }
        }

        "keeps the Python profile when multiple Python markers match" {
            val pythonRoot = createTempDirectory("microsmith-init-detect-python-multi")
            try {
                pythonRoot.resolve("pyproject.toml").writeText("[project]\nname = \"fixture-python\"\n")
                pythonRoot.resolve("requirements.txt").writeText("protobuf==0.0.0\n")

                detectOnboardingProfile(pythonRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = PythonOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pyproject.toml", "requirements.txt"),
                    )
            } finally {
                runCatching { pythonRoot.deleteRecursively() }
            }
        }

        "detects Rust repositories from Cargo.toml at package or workspace roots" {
            val packageRoot = createTempDirectory("microsmith-init-detect-rust-package")
            val workspaceRoot = createTempDirectory("microsmith-init-detect-rust-workspace")
            try {
                packageRoot.resolve("Cargo.toml").writeText(
                    """
                    [package]
                    name = "fixture-rust"
                    version = "0.1.0"
                    edition = "2024"
                    """.trimIndent() + "\n",
                )
                workspaceRoot.resolve("crates/app").createDirectories()
                workspaceRoot.resolve("Cargo.toml").writeText(
                    """
                    [workspace]
                    members = ["crates/app"]
                    """.trimIndent() + "\n",
                )

                detectOnboardingProfile(packageRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RustOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("Cargo.toml"),
                    )
                detectOnboardingProfile(workspaceRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RustOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("Cargo.toml"),
                    )
            } finally {
                runCatching { packageRoot.deleteRecursively() }
                runCatching { workspaceRoot.deleteRecursively() }
            }
        }

        "falls back to the generic profile when a nested Rust crate exists without a root Cargo.toml" {
            val repoRoot = createTempDirectory("microsmith-init-detect-rust-nested-only")
            try {
                repoRoot.resolve("crates/app").createDirectories()
                repoRoot.resolve("crates/app/Cargo.toml").writeText(
                    """
                    [package]
                    name = "fixture-rust-app"
                    version = "0.1.0"
                    edition = "2024"
                    """.trimIndent() + "\n",
                )

                detectOnboardingProfile(repoRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                        matchedMarkers = emptyList(),
                    )
            } finally {
                runCatching { repoRoot.deleteRecursively() }
            }
        }

        "Ruby root gemspec detection skips unreadable repository roots".config(enabled = supportsPosixPermissions()) {
            val rubyRoot = createTempDirectory("microsmith-init-detect-ruby-unreadable-root")
            rubyRoot.resolve("microsmith-ruby-fixture.gemspec").writeText(
                """
                Gem::Specification.new do |spec|
                  spec.name = "microsmith-ruby-fixture"
                  spec.version = "0.1.0"
                end
                """.trimIndent() + "\n",
            )

            val originalPermissions = Files.getPosixFilePermissions(rubyRoot)
            Files.setPosixFilePermissions(rubyRoot, emptySet())
            try {
                detectOnboardingProfile(rubyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.NO_MARKERS_MATCHED,
                        matchedMarkers = emptyList(),
                    )
            } finally {
                runCatching {
                    Files.setPosixFilePermissions(rubyRoot, originalPermissions)
                }
                runCatching { rubyRoot.deleteRecursively() }
            }
        }

        "detects built-in repository profiles and falls back to the generic profile for ambiguous markers" {
            val javaRoot = createTempDirectory("microsmith-init-detect-java")
            val kotlinRoot = createTempDirectory("microsmith-init-detect-kotlin")
            val nodeRoot = createTempDirectory("microsmith-init-detect-node")
            val goRoot = createTempDirectory("microsmith-init-detect-go")
            val pythonRoot = createTempDirectory("microsmith-init-detect-python")
            val rubyRoot = createTempDirectory("microsmith-init-detect-ruby")
            val rustRoot = createTempDirectory("microsmith-init-detect-rust")
            val dotnetRoot = createTempDirectory("microsmith-init-detect-dotnet")
            val mixedRoot = createTempDirectory("microsmith-init-detect-mixed")
            try {
                javaRoot.resolve("pom.xml").writeText("<project />\n")
                javaRoot.resolve("src/main/java/example").createDirectories()
                kotlinRoot.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"2.2.21\" }\n")
                kotlinRoot.resolve("src/main/kotlin/example").createDirectories()
                nodeRoot.resolve("package.json").writeText("""{"name":"fixture-node"}""")
                goRoot.resolve("go.mod").writeText("module example.com/microsmith/fixture\n")
                pythonRoot.resolve("pyproject.toml").writeText("[project]\nname = \"fixture-python\"\n")
                rubyRoot.resolve("gems.rb").writeText("source \"https://rubygems.org\"\n")
                rustRoot.resolve("Cargo.toml").writeText("[package]\nname = \"fixture-rust\"\nversion = \"0.1.0\"\n")
                dotnetRoot.resolve("src/apps/service").createDirectories()
                dotnetRoot.resolve("src/apps/service/Fixture.csproj")
                    .writeText("<Project Sdk=\"Microsoft.NET.Sdk\" />\n")
                mixedRoot.resolve("gems.rb").writeText("source \"https://rubygems.org\"\n")
                mixedRoot.resolve("Cargo.toml").writeText("[package]\nname = \"fixture-rust\"\nversion = \"0.1.0\"\n")

                detectOnboardingProfile(javaRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = JavaOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pom.xml", "src/main/java"),
                    )
                detectOnboardingProfile(kotlinRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = KotlinOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("build.gradle.kts", "src/main/kotlin"),
                    )
                detectOnboardingProfile(nodeRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = NodeOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("package.json"),
                    )
                detectOnboardingProfile(goRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GoOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("go.mod"),
                    )
                detectOnboardingProfile(pythonRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = PythonOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("pyproject.toml"),
                    )
                detectOnboardingProfile(rubyRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RubyOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("gems.rb"),
                    )
                detectOnboardingProfile(rustRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = RustOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("Cargo.toml"),
                    )
                detectOnboardingProfile(dotnetRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = DotnetOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.MATCHED_PROFILE,
                        matchedMarkers = listOf("src/apps/service/Fixture.csproj"),
                    )
                detectOnboardingProfile(mixedRoot) shouldBe
                    OnboardingProfileDetection(
                        profile = GenericOnboardingProfile,
                        selectionReason = OnboardingProfileSelectionReason.AMBIGUOUS_MARKERS,
                        matchedMarkers = listOf("Cargo.toml", "gems.rb"),
                    )
            } finally {
                runCatching { javaRoot.deleteRecursively() }
                runCatching { kotlinRoot.deleteRecursively() }
                runCatching { nodeRoot.deleteRecursively() }
                runCatching { goRoot.deleteRecursively() }
                runCatching { pythonRoot.deleteRecursively() }
                runCatching { rubyRoot.deleteRecursively() }
                runCatching { rustRoot.deleteRecursively() }
                runCatching { dotnetRoot.deleteRecursively() }
                runCatching { mixedRoot.deleteRecursively() }
            }
        }
    })

private val UNUSED_PROJECT_ROOT: Path = Path.of(".")

private fun supportsPosixPermissions(): Boolean =
    !runningOnWindows() && FileSystems.getDefault().supportedFileAttributeViews().contains("posix")

private fun detectorWithDotnetMarkerFinder(dotnetMarkerFinder: (Path) -> String?): OnboardingProfileDetector =
    OnboardingProfileDetector(
        matchers = BuiltInOnboardingProfileMatchers.all(dotnetMarkerFinder),
    )

private fun runningOnWindows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
