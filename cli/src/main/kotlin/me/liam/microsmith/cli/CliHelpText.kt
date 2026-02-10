package me.liam.microsmith.cli

internal const val HELP_TEXT = """
Microsmith CLI (Phase 3)

Usage:
  microsmith run <script.microsmith.kts> --out <output-dir> [--var <name=value>]... [--flag <name>]...
                 [--plugin <group:artifact:version>]... [--plugin-jar <path>]...
                 [--offline] [--repository <uri>]
  microsmith --help
"""
