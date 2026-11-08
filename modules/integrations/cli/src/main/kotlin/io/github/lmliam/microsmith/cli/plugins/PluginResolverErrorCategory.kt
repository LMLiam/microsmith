package io.github.lmliam.microsmith.cli.plugins

internal enum class PluginResolverErrorCategory(val code: String) {
    OFFLINE_CACHE_MISS("offline-cache-miss"),
    AUTHENTICATION("authentication"),
    DEPENDENCY_RESOLUTION("dependency-resolution"),
    LOCKFILE("lockfile"),
    REPOSITORY_POLICY("repository-policy"),
    ROOT_ARTIFACT_MISSING("root-artifact-missing"),
}
