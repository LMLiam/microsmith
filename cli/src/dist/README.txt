Microsmith CLI distribution
===========================

Contents:
- lib/ : executable fat jar with built-in generators and emitters
- bin/microsmith : launcher for Linux/macOS
- bin/microsmith.bat : launcher for Windows
- bundled-plugins.lock : bundled plugin profile (pinned to this CLI release version)

Runtime requirements:
- Java 24+ (Temurin 24 recommended)

Quick checks:
- bin/microsmith --help
- bin/microsmith --version
- bin/microsmith init
- bin/microsmith run build.microsmith.kts --out ./generated
- bin/microsmith run schema.microsmith.kts --out ./generated
- bin/microsmith ide refresh
- bin/microsmith ide doctor
- cat bundled-plugins.lock

Bundled vs external plugins:
- bundled plugins run without network and do not require --plugin flags
- external plugins remain optional via --plugin <group:artifact:version> or --plugin-jar <path>
