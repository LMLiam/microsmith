param(
    [string]$Version = $env:MICROSMITH_INSTALL_VERSION,
    [string]$InstallRoot = $(if ($env:MICROSMITH_INSTALL_ROOT) { $env:MICROSMITH_INSTALL_ROOT } else { Join-Path $HOME ".microsmith" }),
    [string]$BinDir = $env:MICROSMITH_INSTALL_BIN_DIR,
    [string]$Repository = $(if ($env:MICROSMITH_INSTALL_REPOSITORY) { $env:MICROSMITH_INSTALL_REPOSITORY } else { "LMLiam/microsmith" }),
    [string]$DistUrl = $env:MICROSMITH_INSTALL_DIST_URL,
    [string]$DistFile = $env:MICROSMITH_INSTALL_DIST_FILE,
    [string]$DistSha256 = $env:MICROSMITH_INSTALL_DIST_SHA256,
    [string]$RuntimeUrl = $env:MICROSMITH_INSTALL_RUNTIME_URL,
    [string]$RuntimeFile = $env:MICROSMITH_INSTALL_RUNTIME_FILE,
    [string]$RuntimeSha256 = $env:MICROSMITH_INSTALL_RUNTIME_SHA256,
    [switch]$ForceRuntimeProvision,
    [switch]$SkipRuntimeProvision,
    [switch]$NoProfileUpdate
)

$ErrorActionPreference = "Stop"
$MinimumJavaFeature = 24
$stagedInstallDir = $null
$backupInstallDir = $null

function Write-Info {
    param([string]$Message)
    Write-Host "[microsmith-install] $Message"
}

function Fail {
    param([string]$Message)
    throw "[microsmith-install] error: $Message"
}

function Get-ChecksumFromText {
    param([string]$Text)
    if ([string]::IsNullOrWhiteSpace($Text)) {
        return ""
    }

    return ($Text -split "\s+")[0].Trim()
}

function Get-Sha256 {
    param([string]$Path)
    return (Get-FileHash -Path $Path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Get-SourceText {
    param([string]$SourceRef)
    if ($SourceRef.StartsWith("http://") -or $SourceRef.StartsWith("https://")) {
        return (Invoke-WebRequest -Uri $SourceRef -UseBasicParsing).Content
    }

    if ($SourceRef.StartsWith("file://")) {
        return Get-Content -Path $SourceRef.Substring(7) -Raw
    }

    return Get-Content -Path $SourceRef -Raw
}

function Copy-OrDownload {
    param(
        [string]$SourceRef,
        [string]$Destination
    )

    if ($SourceRef.StartsWith("http://") -or $SourceRef.StartsWith("https://")) {
        Invoke-WebRequest -Uri $SourceRef -OutFile $Destination -UseBasicParsing
        return
    }

    if ($SourceRef.StartsWith("file://")) {
        Copy-Item -Path $SourceRef.Substring(7) -Destination $Destination -Force
        return
    }

    Copy-Item -Path $SourceRef -Destination $Destination -Force
}

function Get-SourceLeafName {
    param([string]$SourceRef)
    $trimmed = $SourceRef
    if ($trimmed.Contains("?")) {
        $trimmed = $trimmed.Split("?")[0]
    }
    if ($trimmed.StartsWith("file://")) {
        $trimmed = $trimmed.Substring(7)
    }
    return Split-Path -Path $trimmed -Leaf
}

function Infer-VersionFromText {
    param([string]$Text)
    $match = [regex]::Match($Text, "microsmith-cli-([0-9A-Za-z._+\-]+)-dist\.(zip|tar\.gz)")
    if ($match.Success) {
        return $match.Groups[1].Value
    }
    return ""
}

function Resolve-LatestVersion {
    param([string]$RepositoryName)
    $response = Invoke-RestMethod -Uri "https://api.github.com/repos/$RepositoryName/releases/latest"
    if (-not $response.tag_name) {
        Fail "Unable to determine latest release tag from GitHub API."
    }

    return $response.tag_name.TrimStart("v")
}

function Get-JavaFeature {
    param([string]$JavaCommand)
    try {
        $versionLine = (& $JavaCommand -version 2>&1 | Select-Object -First 1)
    } catch {
        return 0
    }

    $line = "$versionLine"
    $quoted = [regex]::Match($line, '"([^"]+)"')
    if (-not $quoted.Success) {
        return 0
    }

    $raw = $quoted.Groups[1].Value
    $first = $raw.Split(".")[0]
    if ($first -eq "1") {
        $second = $raw.Split(".")[1]
        $digits = [regex]::Match($second, "^\d+").Value
        return $(if ($digits) { [int]$digits } else { 0 })
    }

    $leadingDigits = [regex]::Match($first, "^\d+").Value
    return $(if ($leadingDigits) { [int]$leadingDigits } else { 0 })
}

function Resolve-SystemJavaCommand {
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($java) {
        return $java.Source
    }

    return ""
}

function Detect-Arch {
    $arch = $env:PROCESSOR_ARCHITECTURE
    if ($arch -eq "AMD64" -or $arch -eq "x86_64") {
        return "x64"
    }

    if ($arch -eq "ARM64") {
        return "aarch64"
    }

    Fail "Unsupported CPU architecture '$arch'."
}

function Update-UserPath {
    param([string]$BinDirectory)
    if ($NoProfileUpdate.IsPresent) {
        return
    }

    $userPath = [Environment]::GetEnvironmentVariable("Path", "User")
    if (-not $userPath) {
        $userPath = ""
    }

    $segments = $userPath.Split(";", [System.StringSplitOptions]::RemoveEmptyEntries)
    if ($segments -contains $BinDirectory) {
        return
    }

    $newPath = if ([string]::IsNullOrWhiteSpace($userPath)) {
        $BinDirectory
    } else {
        "$BinDirectory;$userPath"
    }
    [Environment]::SetEnvironmentVariable("Path", $newPath, "User")
}

if ([string]::IsNullOrWhiteSpace($BinDir)) {
    $BinDir = Join-Path $InstallRoot "bin"
}

if ($ForceRuntimeProvision.IsPresent -and $SkipRuntimeProvision.IsPresent) {
    Fail "--force-runtime-provision and --skip-runtime-provision cannot be used together."
}

if ([string]::IsNullOrWhiteSpace($Version)) {
    if ($DistFile) {
        $Version = Infer-VersionFromText $DistFile
    } elseif ($DistUrl) {
        $Version = Infer-VersionFromText $DistUrl
    }
}
if ([string]::IsNullOrWhiteSpace($Version)) {
    $Version = Resolve-LatestVersion -RepositoryName $Repository
}

if ([string]::IsNullOrWhiteSpace($DistUrl) -and [string]::IsNullOrWhiteSpace($DistFile)) {
    $DistUrl = "https://github.com/$Repository/releases/download/v$Version/microsmith-cli-$Version-dist.zip"
}

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("microsmith-install-" + [System.Guid]::NewGuid().ToString("N"))
New-Item -Path $tempRoot -ItemType Directory | Out-Null
try {
    New-Item -Path $InstallRoot -ItemType Directory -Force | Out-Null

    $distSourceRef = if ($DistFile) { $DistFile } else { $DistUrl }
    $distLeafName = Get-SourceLeafName -SourceRef $distSourceRef
    if ([string]::IsNullOrWhiteSpace($distLeafName)) {
        $distLeafName = "microsmith-cli-dist.zip"
    }
    $distArchive = Join-Path $tempRoot $distLeafName
    if ($DistFile) {
        Write-Info "Using local distribution archive: $DistFile"
        Copy-OrDownload -SourceRef $DistFile -Destination $distArchive
    } else {
        Write-Info "Downloading CLI distribution: $DistUrl"
        Copy-OrDownload -SourceRef $DistUrl -Destination $distArchive
    }

    if ([string]::IsNullOrWhiteSpace($DistSha256)) {
        if ($DistFile -and (Test-Path "$DistFile.sha256")) {
            $DistSha256 = Get-ChecksumFromText (Get-Content -Path "$DistFile.sha256" -Raw)
        } elseif ($DistUrl) {
            try {
                $DistSha256 = Get-ChecksumFromText (Get-SourceText -SourceRef "$DistUrl.sha256")
            } catch {
                $DistSha256 = ""
            }
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($DistSha256)) {
        $actualDistSha256 = Get-Sha256 -Path $distArchive
        if ($actualDistSha256 -ne $DistSha256.ToLowerInvariant()) {
            Fail "Distribution checksum mismatch: expected '$DistSha256', got '$actualDistSha256'."
        }
    } else {
        Write-Warning "[microsmith-install] distribution checksum not available; integrity verification skipped."
    }

    $distExtract = Join-Path $tempRoot "dist-extract"
    Expand-Archive -Path $distArchive -DestinationPath $distExtract -Force
    $distRoot = Get-ChildItem -Path $distExtract -Directory | Select-Object -First 1
    if (-not $distRoot) {
        Fail "Could not locate extracted distribution root."
    }

    $installDir = Join-Path $InstallRoot "installs\microsmith-cli-$Version"
    $stagedInstallDir = Join-Path $InstallRoot ("installs\.microsmith-cli-$Version.staging-" + [System.Guid]::NewGuid().ToString("N"))
    if (Test-Path $stagedInstallDir) {
        Remove-Item -Path $stagedInstallDir -Recurse -Force
    }
    New-Item -ItemType Directory -Path (Split-Path $installDir -Parent) -Force | Out-Null
    Move-Item -Path $distRoot.FullName -Destination $stagedInstallDir

    $systemJavaCommand = Resolve-SystemJavaCommand
    $systemJavaFeature = if ($systemJavaCommand) { Get-JavaFeature -JavaCommand $systemJavaCommand } else { 0 }
    $shouldProvisionRuntime = $ForceRuntimeProvision.IsPresent -or ($systemJavaFeature -lt $MinimumJavaFeature)
    if ($shouldProvisionRuntime) {
        if ($SkipRuntimeProvision.IsPresent) {
            Fail "Runtime provisioning was disabled but Java $MinimumJavaFeature+ is unavailable."
        }

        $runtimeSourceUrl = $RuntimeUrl
        $runtimeSourceFile = $RuntimeFile
        $runtimeChecksum = $RuntimeSha256
        if (-not $runtimeSourceUrl -and -not $runtimeSourceFile) {
            $arch = Detect-Arch
            $metadata = Invoke-RestMethod -Uri "https://api.adoptium.net/v3/assets/latest/24/hotspot?architecture=$arch&heap_size=normal&image_type=jre&jvm_impl=hotspot&os=windows&vendor=eclipse"
            if (-not $metadata -or -not $metadata[0].binary.package.link) {
                Fail "Unable to resolve runtime metadata from Adoptium API."
            }
            $runtimeSourceUrl = $metadata[0].binary.package.link
            if (-not $runtimeChecksum) {
                $runtimeChecksum = $metadata[0].binary.package.checksum
            }
        }

        $runtimeTarget = Join-Path $stagedInstallDir "runtime"
        if (Test-Path $runtimeTarget) {
            Remove-Item -Path $runtimeTarget -Recurse -Force
        }
        if ($runtimeSourceFile -and (Test-Path $runtimeSourceFile) -and (Get-Item $runtimeSourceFile).PSIsContainer) {
            Write-Info "Using local runtime directory: $runtimeSourceFile"
            Copy-Item -Path $runtimeSourceFile -Destination $runtimeTarget -Recurse
        } else {
            $runtimeSourceRef = if ($runtimeSourceFile) { $runtimeSourceFile } else { $runtimeSourceUrl }
            $runtimeLeafName = Get-SourceLeafName -SourceRef $runtimeSourceRef
            if ([string]::IsNullOrWhiteSpace($runtimeLeafName)) {
                $runtimeLeafName = "runtime.zip"
            }
            $runtimeArchive = Join-Path $tempRoot $runtimeLeafName
            if ($runtimeSourceFile) {
                Write-Info "Using local runtime archive: $runtimeSourceFile"
                Copy-OrDownload -SourceRef $runtimeSourceFile -Destination $runtimeArchive
                if (-not $runtimeChecksum -and (Test-Path "$runtimeSourceFile.sha256")) {
                    $runtimeChecksum = Get-ChecksumFromText (Get-Content -Path "$runtimeSourceFile.sha256" -Raw)
                }
            } else {
                Write-Info "Downloading Java $MinimumJavaFeature runtime archive."
                Copy-OrDownload -SourceRef $runtimeSourceUrl -Destination $runtimeArchive
                if (-not $runtimeChecksum) {
                    try {
                        $runtimeChecksum = Get-ChecksumFromText (Get-SourceText -SourceRef "$runtimeSourceUrl.sha256")
                    } catch {
                        $runtimeChecksum = ""
                    }
                }
            }

            if ([string]::IsNullOrWhiteSpace($runtimeChecksum)) {
                Fail "Runtime checksum not available for verification."
            }

            $actualRuntimeSha256 = Get-Sha256 -Path $runtimeArchive
            if ($actualRuntimeSha256 -ne $runtimeChecksum.ToLowerInvariant()) {
                Fail "Runtime checksum mismatch: expected '$runtimeChecksum', got '$actualRuntimeSha256'."
            }

            $runtimeExtract = Join-Path $tempRoot "runtime-extract"
            Expand-Archive -Path $runtimeArchive -DestinationPath $runtimeExtract -Force
            $javaExe = Get-ChildItem -Path $runtimeExtract -Filter java.exe -Recurse | Select-Object -First 1
            if (-not $javaExe) {
                Fail "Unable to locate runtime java.exe in extracted archive."
            }

            $runtimeHome = Split-Path (Split-Path $javaExe.FullName -Parent) -Parent
            Move-Item -Path $runtimeHome -Destination $runtimeTarget
        }
    }

    $runtimeJava = Join-Path $stagedInstallDir "runtime\bin\java.exe"
    $finalJavaFeature = if (Test-Path $runtimeJava) {
        Get-JavaFeature -JavaCommand $runtimeJava
    } elseif ($systemJavaCommand) {
        $systemJavaFeature
    } else {
        0
    }
    if ($finalJavaFeature -lt $MinimumJavaFeature) {
        Fail "No usable Java $MinimumJavaFeature+ runtime available after install."
    }

    $stagedLauncher = Join-Path $stagedInstallDir "bin\microsmith.bat"
    $versionOutput = (& $stagedLauncher --version 2>&1 | Out-String).Trim()
    $versionExitCode = $LASTEXITCODE
    if ($versionExitCode -ne 0) {
        $healthFailure = if ([string]::IsNullOrWhiteSpace($versionOutput)) { "no output" } else { $versionOutput }
        Fail "Installed CLI failed health check (--version): $healthFailure"
    }
    if ([string]::IsNullOrWhiteSpace($versionOutput)) {
        Fail "Installed CLI failed health check (--version) with no output."
    }

    if (Test-Path $installDir) {
        $backupInstallDir = Join-Path $InstallRoot ("installs\.microsmith-cli-$Version.backup-" + [System.Guid]::NewGuid().ToString("N"))
        if (Test-Path $backupInstallDir) {
            Remove-Item -Path $backupInstallDir -Recurse -Force
        }
        Move-Item -Path $installDir -Destination $backupInstallDir
    }

    try {
        Move-Item -Path $stagedInstallDir -Destination $installDir
        $stagedInstallDir = $null
    } catch {
        if ($backupInstallDir -and (Test-Path $backupInstallDir) -and -not (Test-Path $installDir)) {
            Move-Item -Path $backupInstallDir -Destination $installDir
            $backupInstallDir = $null
        }
        throw
    }

    if ($backupInstallDir -and (Test-Path $backupInstallDir)) {
        Remove-Item -Path $backupInstallDir -Recurse -Force
        $backupInstallDir = $null
    }

    New-Item -Path $BinDir -ItemType Directory -Force | Out-Null
    $shimPath = Join-Path $BinDir "microsmith.cmd"
    $shimContent = @"
@echo off
setlocal
"$installDir\bin\microsmith.bat" %*
exit /b %ERRORLEVEL%
"@
    Set-Content -Path $shimPath -Value $shimContent -Encoding ASCII

    Update-UserPath -BinDirectory $BinDir
    $env:Path = "$BinDir;$env:Path"

    Write-Info "Installed $versionOutput at $InstallRoot."
    if (Get-Command microsmith -ErrorAction SilentlyContinue) {
        $globalVersion = (& microsmith --version 2>&1 | Out-String).Trim()
        $globalExitCode = $LASTEXITCODE
        if ($globalExitCode -ne 0) {
            Write-Info "Global command is present but failed version check in current shell."
        } elseif ([string]::IsNullOrWhiteSpace($globalVersion)) {
            Write-Info "Global command is present but returned no version output in current shell."
        } else {
            Write-Info "Global command available: $globalVersion"
        }
    } else {
        Write-Info "Use '$shimPath' directly, or open a new shell to pick up PATH updates."
    }
} finally {
    if (Test-Path $tempRoot) {
        Remove-Item -Path $tempRoot -Recurse -Force
    }
    if ($stagedInstallDir -and (Test-Path $stagedInstallDir)) {
        Remove-Item -Path $stagedInstallDir -Recurse -Force
    }
    if ($backupInstallDir -and (Test-Path $backupInstallDir)) {
        Remove-Item -Path $backupInstallDir -Recurse -Force
    }
}
