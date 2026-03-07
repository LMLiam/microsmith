## Microsmith CLI Runtime Notes

- CLI artifacts include:
  - executable fat jar (`microsmith-cli-<version>-all.jar`)
  - distribution archives with launchers for Linux/macOS (`bin/microsmith`) and Windows (`bin/microsmith.bat`)
  - installer scripts (`microsmith-install.sh`, `microsmith-install.ps1`)
  - bundled plugin profile (`bundled-plugins.lock`)
- Java 24 or newer is required for manual channels; installer scripts provision Java 24 automatically when needed.
- Installation options:
  - run fat jar directly: `java -jar microsmith-cli-<version>-all.jar --help`
  - unpack distribution archive and run launcher script
  - install through canonical OS installer scripts (`docs/cli/install.md`)
  - bootstrap repository defaults: `microsmith init`
  - verify install/runtime health: run the installed shim directly (for example, `~/.microsmith/bin/microsmith --version`) or open a new shell and use `microsmith --version`
  - generate JetBrains IDE helper metadata: `microsmith ide refresh`
  - validate JetBrains IDE helper health: `microsmith ide doctor`
- Bundled plugin policy:
  - bundled plugin coordinates are pinned to the CLI release version
  - bundled profile content must stay checksum-verifiable and deterministic across artifacts
- Security defaults remain enabled in CLI runtime:
  - script dependency directives blocked by default
  - repository policy and checksum controls for plugin resolution
  - output boundary enforcement under configured `--out` path

Release checklist additions:
- review bundled plugin composition before cutting release artifacts
- run `./gradlew :cli:releaseArtifacts` (includes bundled metadata, installer assets, and checksums)
- confirm release notes document bundled-vs-external plugin behavior changes
