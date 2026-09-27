package io.github.lmliam.microsmith.cli.plugins.lockfile

internal data class ParsedLockfile(val version: Int, val entries: List<LockEntry>)
