package me.liam.microsmith.cli

internal const val HELP_TEXT = """
Microsmith CLI

Usage:
  microsmith run <script.microsmith.kts> --out <output-dir> [--var <name=value>]... [--flag <name>]...
                 [--plugin <group:artifact:version>]... [--plugin-jar <path>]...
                 [--offline] [--repository <uri>] [--isolation <classloader|process>]
                 [--diagnostics <text|json>] [--verbose] [--event-log <path>]
  microsmith doctor [--diagnostics <text|json>] [--verbose]
  microsmith --help

Security policy env vars:
  MICROSMITH_REPOSITORY_ALLOWLIST   Comma-separated additional allowed repository base URIs.
  MICROSMITH_ALLOW_FILE_REPOSITORIES Set to true to allow file:// repositories for plugin coordinates.
  MICROSMITH_PLUGIN_ALLOWLIST_FILE  Path to checksum allowlist file (<kind>|<key>|<sha256>).
  MICROSMITH_SCRIPT_CACHE_DIR       Override script compilation cache directory.
  MICROSMITH_PLUGIN_CACHE_DIR       Override plugin resolution cache directory.
"""
