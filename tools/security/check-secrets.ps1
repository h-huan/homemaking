param(
    [string]$BaseRef = 'origin/saas-platform',
    [string]$Gitleaks = 'gitleaks'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$scanner = (Get-Command $Gitleaks -ErrorAction Stop).Source
$version = & $scanner version
if ($LASTEXITCODE -ne 0 -or $version.Trim() -ne '8.30.1') {
    throw 'Use the verified Gitleaks 8.30.1 release; see tools/security/README.md.'
}
$tempRoot = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$scanRoot = Join-Path $tempRoot ('hm-secrets-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $scanRoot | Out-Null
Push-Location $repo
try {
    $baseCommit = & git rev-parse --verify "$BaseRef^{commit}"
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve BaseRef. Fetch the target branch first.' }
    & git merge-base --is-ancestor $baseCommit HEAD
    if ($LASTEXITCODE -ne 0) { throw 'BaseRef is not an ancestor of HEAD; review the branch before scanning.' }
    # Include tracked edits and new non-ignored files, not node_modules or private .env files.
    $paths = & git -c core.quotepath=false ls-files --cached --others --exclude-standard
    if ($LASTEXITCODE -ne 0) { throw 'Cannot enumerate repository files.' }
    foreach ($relative in ($paths | Sort-Object -Unique)) {
        $source = Join-Path $repo $relative
        if (!(Test-Path -LiteralPath $source -PathType Leaf)) { continue }
        $target = [IO.Path]::GetFullPath((Join-Path $scanRoot $relative))
        if (!$target.StartsWith($scanRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'Invalid repository path.'
        }
        New-Item -ItemType Directory -Force -Path ([IO.Path]::GetDirectoryName($target)) | Out-Null
        Copy-Item -LiteralPath $source -Destination $target
    }
    & $scanner dir $scanRoot --config (Join-Path $repo '.gitleaks.toml') --redact --no-banner
    if ($LASTEXITCODE -ne 0) { throw 'Current files failed secret scanning. Do not push.' }
    & $scanner git $repo --config (Join-Path $repo '.gitleaks.toml') --log-opts="$baseCommit..HEAD" --redact --no-banner
    if ($LASTEXITCODE -ne 0) { throw 'Outgoing history failed secret scanning. Clean the introducing commits.' }
    Write-Output 'PASS: current files and commits after BaseRef contain no scanner findings.'
} finally {
    Pop-Location
    $resolved = [IO.Path]::GetFullPath($scanRoot)
    if (!$resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase) -or
        !([IO.Path]::GetFileName($resolved)).StartsWith('hm-secrets-')) {
        throw 'Refusing cleanup outside the temporary scan directory.'
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
