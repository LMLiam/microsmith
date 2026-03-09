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
            dotnetMatcher(dotnetMarkerFinder),
        )
    }

    private fun dotnetMatcher(dotnetMarkerFinder: (Path) -> String?): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(DotnetOnboardingProfile) { projectRoot ->
            DotnetOnboardingMarkerFinder.findSafely(projectRoot, dotnetMarkerFinder)?.let(::listOf).orEmpty()
        }
    }

    private fun rootMarkerMatcher(profile: OnboardingProfile, markerFileName: String): OnboardingProfileMatcher {
        return OnboardingProfileMatcher(profile) { projectRoot ->
            if (projectRoot.resolve(markerFileName).isRegularFile()) {
                listOf(markerFileName)
            } else {
                emptyList()
            }
        }
    }
}
