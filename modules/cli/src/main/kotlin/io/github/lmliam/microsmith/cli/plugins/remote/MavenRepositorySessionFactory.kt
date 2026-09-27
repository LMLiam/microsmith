package io.github.lmliam.microsmith.cli.plugins.remote

import java.nio.file.Path
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.repository.LocalRepository

internal class MavenRepositorySessionFactory {
    fun create(
        repositorySystem: RepositorySystem,
        localRepositoryRoot: Path,
        offline: Boolean,
    ): RepositorySystemSession {
        val session = DefaultRepositorySystemSession()
        val localRepository = LocalRepository(localRepositoryRoot.toFile())
        session.isOffline = offline
        session.localRepositoryManager = repositorySystem.newLocalRepositoryManager(session, localRepository)
        return session
    }
}
