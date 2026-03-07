package me.liam.microsmith.cli.plugins

import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.repository.LocalRepository
import java.nio.file.Path

internal class MavenRepositorySessionFactory {
    fun create(
        repositorySystem: RepositorySystem,
        localRepositoryRoot: Path,
        offline: Boolean,
    ): RepositorySystemSession {
        val session = DefaultRepositorySystemSession()
        val localRepository = LocalRepository(localRepositoryRoot.toFile())
        session.setOffline(offline)
        session.setLocalRepositoryManager(
            repositorySystem.newLocalRepositoryManager(session, localRepository),
        )
        return session
    }
}
