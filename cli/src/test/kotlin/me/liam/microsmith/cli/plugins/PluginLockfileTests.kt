package me.liam.microsmith.cli.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.string.shouldContain
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readLines

@OptIn(ExperimentalPathApi::class)
class PluginLockfileTests :
    StringSpec({
        "writeLockfile persists entries in deterministic sorted order" {
            val tempDir = createTempDirectory("microsmith-plugin-lockfile-write")
            try {
                val lockfilePath = tempDir.resolve("schema.plugins.lock")
                val remoteChecksum = "a".repeat(64)
                val artifactChecksum = "b".repeat(64)
                val localChecksum = "c".repeat(64)

                writeLockfile(
                    lockfilePath = lockfilePath,
                    lockfile = ParsedLockfile(
                        version = LOCKFILE_VERSION,
                        entries = listOf(
                            LockEntry(
                                kind = REMOTE_ARTIFACT_KIND,
                                key = "b/path.jar",
                                checksum = artifactChecksum,
                            ),
                            LockEntry(
                                kind = LOCAL_KIND,
                                key = "plugins/local.jar",
                                checksum = localChecksum,
                            ),
                            LockEntry(
                                kind = REMOTE_KIND,
                                key = "com.acme:plugin:1.0.0",
                                checksum = remoteChecksum,
                            ),
                        ),
                    ),
                )

                lockfilePath.readLines().shouldContainExactly(
                    "version=2",
                    "local|plugins/local.jar|$localChecksum",
                    "remote|com.acme:plugin:1.0.0|$remoteChecksum",
                    "remote-artifact|b/path.jar|$artifactChecksum",
                )
                readLockfile(lockfilePath)?.entries.shouldContainExactly(
                    LockEntry(kind = LOCAL_KIND, key = "plugins/local.jar", checksum = localChecksum),
                    LockEntry(kind = REMOTE_KIND, key = "com.acme:plugin:1.0.0", checksum = remoteChecksum),
                    LockEntry(
                        kind = REMOTE_ARTIFACT_KIND,
                        key = "b/path.jar",
                        checksum = artifactChecksum,
                    ),
                )
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "assertSameRemoteArtifactSet reports both missing and extra graph entries" {
            val lockfile = ParsedLockfile(
                version = LOCKFILE_VERSION,
                entries = listOf(
                    LockEntry(kind = REMOTE_ARTIFACT_KIND, key = "a/path.jar", checksum = "a".repeat(64)),
                    LockEntry(kind = REMOTE_ARTIFACT_KIND, key = "b/path.jar", checksum = "b".repeat(64)),
                ),
            )

            val error =
                shouldThrow<IllegalArgumentException> {
                    lockfile.assertSameRemoteArtifactSet(
                        resolvedKeys = setOf("a/path.jar", "c/path.jar"),
                        lockfilePath = Path.of("schema.plugins.lock"),
                    )
                }

            error.message.shouldContain("Missing from lockfile: c/path.jar")
            error.message.shouldContain("Not present in resolved graph: b/path.jar")
        }
    })
