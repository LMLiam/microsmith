package io.github.lmliam.microsmith.cli.plugins

import kotlin.LazyThreadSafetyMode

internal interface RepositoryCredentialsResolver {
    fun resolve(repositoryUri: String): RepositoryCredentials?

    fun sensitiveValues(): Set<String> = emptySet()
}

internal fun lazyDefaultRepositoryCredentialsResolver(
    resolverFactory: () -> RepositoryCredentialsResolver = ::defaultRepositoryCredentialsResolver,
): RepositoryCredentialsResolver {
    val delegate by lazy(LazyThreadSafetyMode.NONE, resolverFactory)
    return object : RepositoryCredentialsResolver {
        override fun resolve(repositoryUri: String): RepositoryCredentials? = delegate.resolve(repositoryUri)

        override fun sensitiveValues(): Set<String> = delegate.sensitiveValues()
    }
}
