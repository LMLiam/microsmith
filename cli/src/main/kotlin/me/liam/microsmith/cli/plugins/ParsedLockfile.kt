package me.liam.microsmith.cli.plugins

internal data class ParsedLockfile(val version: Int, val entries: List<LockEntry>)
