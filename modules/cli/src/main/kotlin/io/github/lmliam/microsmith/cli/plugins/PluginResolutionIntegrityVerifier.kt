package io.github.lmliam.microsmith.cli.plugins

import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Path

/** Centralizes checksum, allowlist, and lockfile enforcement so orchestration stays behavior-focused. */
internal class PluginResolutionIntegrityVerifier {
    fun loadLockfile(lockfilePath: Path): ParsedLockfile? = withLockfileDiagnostics {
        readLockfile(lockfilePath)
    }

    fun assertSamePluginSet(lockfile: ParsedLockfile?, requestedKeys: Set<LockKey>, lockfilePath: Path) {
        withLockfileDiagnostics {
            lockfile?.assertSamePluginSet(requestedKeys, lockfilePath)
        }
    }

    fun assertSameRemoteArtifactSet(lockfile: ParsedLockfile?, resolvedKeys: Set<String>, lockfilePath: Path) {
        withLockfileDiagnostics {
            lockfile?.assertSameRemoteArtifactSet(resolvedKeys, lockfilePath)
        }
    }

    fun verifyChecksum(
        kind: String,
        key: String,
        actualChecksum: String,
        lockfile: ParsedLockfile?,
        checksumAllowlist: PluginChecksumAllowlist?,
    ) {
        withLockfileDiagnostics {
            lockfile?.verifyChecksum(kind, key, actualChecksum)
        }
        checksumAllowlist?.verifyChecksum(kind, key, actualChecksum)
    }

    fun assertAllowlistCoverage(checksumAllowlist: PluginChecksumAllowlist?, requestedKeys: Set<LockKey>) {
        checksumAllowlist?.assertCovers(requestedKeys)
    }

    fun assertAllowlistCoverage(checksumAllowlist: PluginChecksumAllowlist?, lockEntries: List<LockEntry>) {
        checksumAllowlist?.assertCovers(lockEntries.toLockKeys())
    }

    fun writeGeneratedLockfile(lockfilePath: Path, existingLockfile: ParsedLockfile?, lockEntries: List<LockEntry>) {
        if (existingLockfile != null) {
            return
        }

        writeLockfile(
            lockfilePath = lockfilePath,
            lockfile = ParsedLockfile(version = LOCKFILE_VERSION, entries = lockEntries),
        )
    }

    private inline fun <T> withLockfileDiagnostics(block: () -> T): T = try {
        block()
    } catch (error: IllegalArgumentException) {
        throw error.toLockfileDiagnostic()
    } catch (error: IOException) {
        throw error.toLockfileDiagnostic()
    } catch (error: UncheckedIOException) {
        throw error.toLockfileDiagnostic()
    } catch (error: SecurityException) {
        throw error.toLockfileDiagnostic()
    }

    private fun Throwable.toLockfileDiagnostic(): PluginResolutionDiagnosticException =
        PluginResolutionDiagnosticException(
            category = PluginResolverErrorCategory.LOCKFILE,
            message = message ?: "Plugin lockfile validation failed.",
            cause = this,
        )
}
