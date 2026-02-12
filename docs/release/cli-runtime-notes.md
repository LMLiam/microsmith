## Microsmith CLI Runtime Notes

- CLI artifacts include:
  - executable fat jar (`microsmith-cli-<version>-all.jar`)
  - distribution archives with launchers for Linux/macOS (`bin/microsmith`) and Windows (`bin/microsmith.bat`)
- Java 24 or newer is required when using non-bundled distributions.
- Installation options:
  - run fat jar directly: `java -jar microsmith-cli-<version>-all.jar --help`
  - unpack distribution archive and run launcher script
- Security defaults remain enabled in CLI runtime:
  - script dependency directives blocked by default
  - repository policy and checksum controls for plugin resolution
  - output boundary enforcement under configured `--out` path
