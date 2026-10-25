# CI examples

Set repository or environment variables:

- `MICROSMITH_VERSION`
- `MICROSMITH_INSTALLER_SH_URL`, for example `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.sh`
- `MICROSMITH_INSTALLER_PS1_URL`, for example `https://github.com/LMLiam/microsmith/releases/download/v<version>/microsmith-install.ps1`

## Node repository

```yaml
name: Microsmith Generate
on:
  pull_request:
  push:
    branches: [main]
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
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Run Microsmith
        run: microsmith run build.microsmith.kts
```

## Java repository

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
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Generate protobuf
        run: microsmith run build.microsmith.kts
```

## Go repository

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
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Generate protobuf
        run: microsmith run build.microsmith.kts
```

## Ruby repository

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
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Generate protobuf
        run: microsmith run build.microsmith.kts
```

## Rust repository

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
      - name: Bootstrap Microsmith
        run: microsmith init
      - name: Generate protobuf
        run: microsmith run build.microsmith.kts
```

## .NET repository

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
      - name: Bootstrap Microsmith
        shell: pwsh
        run: microsmith init
      - name: Run Microsmith
        shell: pwsh
        run: microsmith run build.microsmith.kts
```

[Back to the README](../README.md)
