package io.github.lmliam.microsmith.cli

internal const val HELP_TEXT = """
Microsmith CLI

Usage:
  microsmith init [--repo-root <path>] [--force] [--skip-ide-helper]
                 [--diagnostics <text|json>] [--verbose]
  microsmith run <script.microsmith.kts> [--out <output-dir>] [--var <name=value>]... [--flag <name>]...
                 [--plugin <group:artifact:version>]... [--plugin-jar <path>]...
                 [--offline] [--repository <uri>] [--isolation <classloader|process>]
                 [--diagnostics <text|json>] [--verbose] [--event-log <path>]
  microsmith ide refresh [--repo-root <path>] [--diagnostics <text|json>] [--verbose]
  microsmith ide doctor [--repo-root <path>] [--diagnostics <text|json>] [--verbose]
  microsmith doctor [--diagnostics <text|json>] [--verbose]
  microsmith --version
  microsmith --help

Security policy env vars:
  MICROSMITH_REPOSITORY_ALLOWLIST   Comma-separated additional allowed repository base URIs.
  MICROSMITH_ALLOW_FILE_REPOSITORIES Set to true to allow file:// repositories for plugin coordinates.
  MICROSMITH_REPOSITORY_CREDENTIALS_FILE Path to repository credentials file (<repository-uri>|<username>|<password>).
  MICROSMITH_REPOSITORY_USERNAME    Default username for authenticated repository access.
  MICROSMITH_REPOSITORY_PASSWORD    Default password/token for authenticated repository access.
  MICROSMITH_GITHUB_PACKAGES_USER   Username for https://maven.pkg.github.com access.
  MICROSMITH_GITHUB_PACKAGES_TOKEN  Token for https://maven.pkg.github.com access.
  MICROSMITH_PLUGIN_ALLOWLIST_FILE  Path to checksum allowlist file (<kind>|<key>|<sha256>; kind=remote|remote-artifact|local).
  MICROSMITH_SCRIPT_CACHE_DIR       Override script compilation cache directory.
  MICROSMITH_PLUGIN_CACHE_DIR       Override plugin resolution cache directory.
"""
