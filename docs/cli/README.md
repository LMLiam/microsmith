# Microsmith CLI Docs

Phase 5 documentation for distribution and adoption lives in this folder.

- `quickstart-non-gradle.md`: fast setup for Node, Go, and .NET repositories, including CI snippets.
- `jetbrains-ide-helper.md`: `.microsmith/ide` helper generation and indexing guidance for JetBrains IDEs.
- `migration-from-gradle.md`: migration guide for existing Gradle-centric users.
- `troubleshooting.md`: common runtime, resolver, and diagnostics issues.
- `runtime-bundling-evaluation.md`: bundled Java runtime options and rollout recommendation.

Distribution build outputs:

- `:cli:generateBundledPluginCatalog` -> `cli/build/generated/microsmith/bundled-plugins.lock`
- `:cli:shadowJar` -> `cli/build/libs/microsmith-cli-<version>-all.jar`
- `:cli:prepareDist` -> `cli/build/microsmith-cli-dist/`
- `:cli:cliDistZip` -> `cli/build/distributions/microsmith-cli-<version>-dist.zip`
- `:cli:cliDistTar` -> `cli/build/distributions/microsmith-cli-<version>-dist.tar.gz`
- `:cli:verifyDistLayout` -> validates bundled profile + launcher wiring in directory/zip/tar artifacts
- `:cli:distArtifacts` -> `cli/build/release-assets/`
