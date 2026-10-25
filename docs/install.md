# Installation and verification

## macOS and Linux

```bash
VERSION=<microsmith-version>
curl -fsSL -o microsmith-install.sh "https://github.com/LMLiam/microsmith/releases/download/v${VERSION}/microsmith-install.sh"
sh microsmith-install.sh --version "${VERSION}"
```

## Windows (PowerShell)

```powershell
$Version = "<microsmith-version>"
Invoke-WebRequest -Uri "https://github.com/LMLiam/microsmith/releases/download/v$Version/microsmith-install.ps1" -OutFile microsmith-install.ps1
powershell -ExecutionPolicy Bypass -NoProfile -File .\microsmith-install.ps1 -Version $Version
```

## Verify immediately after installation

Use the installed shim path before opening a new shell:

```bash
"$HOME/.microsmith/bin/microsmith" --version
mkdir -p ./microsmith-smoke
"$HOME/.microsmith/bin/microsmith" init --repo-root ./microsmith-smoke
"$HOME/.microsmith/bin/microsmith" run ./microsmith-smoke/build.microsmith.kts --out ./microsmith-smoke/generated
```

```powershell
& (Join-Path $HOME ".microsmith\bin\microsmith.cmd") --version
New-Item -ItemType Directory -Path .\microsmith-smoke -Force | Out-Null
& (Join-Path $HOME ".microsmith\bin\microsmith.cmd") init --repo-root .\microsmith-smoke
& (Join-Path $HOME ".microsmith\bin\microsmith.cmd") run .\microsmith-smoke\build.microsmith.kts --out .\microsmith-smoke\generated
```

After opening a new shell, the bare `microsmith` command is available on `PATH`.

## Integrity verification

Release assets include SHA-256 sidecar files for:

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

Installer diagnostics explicitly cover:

- missing required tools such as `curl`, `tar`, `unzip`, and `python3` when automatic runtime metadata resolution is used
- checksum mismatch for CLI or runtime archives
- unsupported operating systems or architectures
- runtime provisioning failures
- missing Java 24+ when runtime provisioning is disabled

## Manual channels

The canonical onboarding path is the installer, but manual channels remain supported:

- fat jar: `java -jar microsmith-cli-<version>-all.jar --help`
- unpacked Unix distribution: `./microsmith-cli-<version>/bin/microsmith --help`
- unpacked Windows distribution: `.\microsmith-cli-<version>\bin\microsmith.bat --help`

Manual channels require Java 24 or newer.

[Back to the README](../README.md)
