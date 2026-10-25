# CI examples

Set these repository variables before you use the workflows:

| Variable | Value |
|---|---|
| `MICROSMITH_VERSION` | Release version, without the `v` prefix |
| `MICROSMITH_INSTALLER_SH_URL` | `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.sh` |
| `MICROSMITH_INSTALLER_PS1_URL` | `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.ps1` |

The same workflow works for any language. `microsmith init` detects the repository profile, and `microsmith run` generates the output.

## Unix

```yaml
name: Microsmith Generate
on:
  pull_request:
jobs:
  generate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - name: Install Microsmith CLI
        run: |
          curl -fsSL -o microsmith-install.sh "$MICROSMITH_INSTALLER_SH_URL"
          sh microsmith-install.sh --version "$MICROSMITH_VERSION"
          echo "$HOME/.microsmith/bin" >> "$GITHUB_PATH"
      - name: Generate
        run: |
          microsmith init
          microsmith run build.microsmith.kts
```

## Windows

```yaml
name: Microsmith Generate
on:
  pull_request:
jobs:
  generate:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v5
      - name: Install Microsmith CLI
        shell: pwsh
        run: |
          Invoke-WebRequest -Uri $env:MICROSMITH_INSTALLER_PS1_URL -OutFile microsmith-install.ps1
          .\microsmith-install.ps1 -Version $env:MICROSMITH_VERSION
          Add-Content -Path $env:GITHUB_PATH -Value (Join-Path $HOME ".microsmith\bin")
      - name: Generate
        shell: pwsh
        run: |
          microsmith init
          microsmith run build.microsmith.kts
```

## Gradle and Maven repositories

A repository that already uses the native plugin does not need the CLI. Use the Gradle plugin task or the Maven goal directly. See [gradle.md](gradle.md) and [maven.md](maven.md).

[Back to the README](../README.md)
