package me.liam.microsmith.cli

internal const val HELP_TEXT = """
Microsmith CLI (Phase 4)

Usage:
  microsmith run <script.microsmith.kts> --out <output-dir> [--var <name=value>]... [--flag <name>]...
                 [--plugin <group:artifact:version>]... [--plugin-jar <path>]...
                 [--offline] [--repository <uri>] [--isolation <classloader|process>]
  microsmith --help

Security policy env vars:
  MICROSMITH_REPOSITORY_ALLOWLIST   Comma-separated additional allowed repository base URIs.
  MICROSMITH_PLUGIN_ALLOWLIST_FILE  Path to checksum allowlist file (<kind>|<key>|<sha256>).
"""
