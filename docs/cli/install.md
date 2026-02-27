# Microsmith CLI Installation (Canonical Channels)

This document defines the primary installation paths for clean-machine onboarding.

## Primary install channel

The primary channel installs the CLI distribution, verifies checksums when available, and provisions Java 24 runtime automatically when needed.

### macOS

```bash
VERSION=<microsmith-version>
curl -fsSL -o microsmith-install.sh "https://github.com/LMLiam/microsmith/releases/download/v${VERSION}/microsmith-install.sh"
sh microsmith-install.sh --version "${VERSION}"
```

### Linux

```bash
VERSION=<microsmith-version>
curl -fsSL -o microsmith-install.sh "https://github.com/LMLiam/microsmith/releases/download/v${VERSION}/microsmith-install.sh"
sh microsmith-install.sh --version "${VERSION}"
```

### Windows (PowerShell)

```powershell
$Version = "<microsmith-version>"
Invoke-WebRequest -Uri "https://github.com/LMLiam/microsmith/releases/download/v$Version/microsmith-install.ps1" -OutFile microsmith-install.ps1
powershell -ExecutionPolicy Bypass -NoProfile -File .\microsmith-install.ps1 -Version $Version
```

Post-install checks:

```bash
microsmith --version
microsmith init --non-interactive --yes
```

```powershell
microsmith --version
microsmith init --non-interactive --yes
```

## Integrity verification guidance

Release assets include SHA-256 sidecar files (`*.sha256`) for:

- `microsmith-cli-<version>-all.jar`
- `microsmith-cli-<version>-dist.zip`
- `microsmith-cli-<version>-dist.tar.gz`
- `microsmith-install.sh`
- `microsmith-install.ps1`

Manual verification examples:

```bash
shasum -a 256 microsmith-install.sh
cat microsmith-install.sh.sha256
```

```bash
shasum -a 256 microsmith-cli-<version>-dist.tar.gz
cat microsmith-cli-<version>-dist.tar.gz.sha256
```

```powershell
Get-FileHash .\microsmith-install.ps1 -Algorithm SHA256
Get-Content .\microsmith-install.ps1.sha256
```

```powershell
Get-FileHash .\microsmith-cli-<version>-dist.zip -Algorithm SHA256
Get-Content .\microsmith-cli-<version>-dist.zip.sha256
```

The installer scripts verify checksums when a sidecar file or explicit checksum value is available.

## Secondary/manual channels (non-primary)

These channels remain supported but are not the default onboarding path:

- Run fat jar directly: `java -jar microsmith-cli-<version>-all.jar --help`
- Unpack distribution and run launcher:
  - Unix: `./microsmith-cli-<version>/bin/microsmith --help`
  - Windows: `.\microsmith-cli-<version>\bin\microsmith.bat --help`

Use these channels for air-gapped workflows or custom packaging constraints.

## Installer diagnostics

Installer scripts emit explicit diagnostics for:

- missing required tools (`curl`, `tar`, `unzip` where applicable)
- checksum mismatch for CLI/runtime archives
- unsupported OS/architecture
- runtime provisioning failures
- missing Java 24+ when runtime provisioning is disabled
