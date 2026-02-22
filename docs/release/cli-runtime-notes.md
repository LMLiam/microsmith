## Microsmith CLI Runtime Notes

- CLI artifacts include:
  - executable fat jar (`microsmith-cli-<version>-all.jar`)
  - distribution archives with launchers for Linux/macOS (`bin/microsmith`) and Windows (`bin/microsmith.bat`)
  - bundled plugin profile (`bundled-plugins.lock`)
- Java 24 or newer is required when using non-bundled distributions.
- Installation options:
  - run fat jar directly: `java -jar microsmith-cli-<version>-all.jar --help`
  - unpack distribution archive and run launcher script
  - generate JetBrains IDE helper metadata: `microsmith ide refresh`
- Bundled plugin policy:
  - bundled plugin coordinates are pinned to the CLI release version
  - bundled profile content must stay checksum-verifiable and deterministic across artifacts
- Security defaults remain enabled in CLI runtime:
  - script dependency directives blocked by default
  - repository policy and checksum controls for plugin resolution
  - output boundary enforcement under configured `--out` path

Release checklist additions:
- review bundled plugin composition before cutting release artifacts
- run `./gradlew :cli:distArtifacts` (includes bundled metadata and archive integrity checks)
- confirm release notes document bundled-vs-external plugin behavior changes
