package io.github.lmliam.microsmith.cli.init

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal object BuiltInOnboardingProfileMatchers {
    fun all(
        dotnetMarkerFinder: (Path) -> String? = DotnetOnboardingMarkerFinder::find,
    ): List<OnboardingProfileMatcher> {
        return listOf(
            rootMarkerMatcher(NodeOnboardingProfile, "package.json"),
            rootMarkerMatcher(GoOnboardingProfile, "go.mod"),
            javaMatcher(),
            kotlinMatcher(),
            scalaMatcher(),
            rootMarkerMatcher(
                PythonOnboardingProfile,
                "pyproject.toml",
                "requirements.txt",
                "setup.py",
                "setup.cfg",
            ),
            rootMarkerMatcher(RubyOnboardingProfile, "Gemfile", "gems.rb"),
            rubyGemspecMatcher(),
            rootMarkerMatcher(RustOnboardingProfile, "Cargo.toml"),
            dotnetMatcher(dotnetMarkerFinder),
        )
    }

    private fun dotnetMatcher(dotnetMarkerFinder: (Path) -> String?): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(DotnetOnboardingProfile) { projectRoot ->
            findDotnetMarkers(projectRoot, dotnetMarkerFinder)
        }
    }

    private fun rootMarkerMatcher(
        profile: OnboardingProfile,
        vararg markerFileNames: String,
    ): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(profile) { projectRoot ->
            markerFileNames.filter { markerFileName ->
                projectRoot.resolve(markerFileName).isRegularFile()
            }
        }
    }

    private fun rubyGemspecMatcher(): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(RubyOnboardingProfile) { projectRoot ->
            RubyOnboardingMarkerFinder.find(projectRoot)
        }
    }

    private fun javaMatcher(): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(JavaOnboardingProfile) { projectRoot ->
            JavaOnboardingMarkerFinder.find(projectRoot)
        }
    }

    private fun kotlinMatcher(): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(KotlinOnboardingProfile) { projectRoot ->
            KotlinOnboardingMarkerFinder.find(projectRoot)
        }
    }

    private fun scalaMatcher(): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(ScalaOnboardingProfile) { projectRoot ->
            ScalaOnboardingMarkerFinder.find(projectRoot)
        }
    }

    private fun findDotnetMarkers(projectRoot: Path, dotnetMarkerFinder: (Path) -> String?): List<String> {
        return try {
            dotnetMarkerFinder(projectRoot)?.let(::listOf).orEmpty()
        } catch (_: IOException) {
            emptyList()
        } catch (_: UncheckedIOException) {
            emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }
}
