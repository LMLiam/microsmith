package me.liam.microsmith.cli.init

import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal object BuiltInOnboardingProfileMatchers {
    fun all(
        dotnetMarkerFinder: (Path) -> String? = DotnetOnboardingMarkerFinder::find,
    ): List<OnboardingProfileMatcher> {
        return listOf(
            rootMarkerMatcher(NodeOnboardingProfile, "package.json"),
            rootMarkerMatcher(GoOnboardingProfile, "go.mod"),
            rootMarkerMatcher(
                PythonOnboardingProfile,
                "pyproject.toml",
                "requirements.txt",
                "setup.py",
                "setup.cfg",
            ),
            rootMarkerMatcher(RubyOnboardingProfile, "Gemfile"),
            rubyGemspecMatcher(),
            rootMarkerMatcher(RustOnboardingProfile, "Cargo.toml"),
            dotnetMatcher(dotnetMarkerFinder),
        )
    }

    private fun dotnetMatcher(dotnetMarkerFinder: (Path) -> String?): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(DotnetOnboardingProfile) { projectRoot ->
            DotnetOnboardingMarkerFinder.findSafely(projectRoot, dotnetMarkerFinder)?.let(::listOf).orEmpty()
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
            RubyOnboardingMarkerFinder.findRootGemspecs(projectRoot)
        }
    }
}
