$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$bundle=Join-Path $projectRoot 'hm-server/target/hm-server.jar'
$libraries=Join-Path $projectRoot '.runtime/bootstrap/lib'
New-Item -ItemType Directory -Path $libraries -Force | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive=[IO.Compression.ZipFile]::OpenRead($bundle)
try {
  foreach($entry in $archive.Entries) {
    if($entry.FullName.StartsWith('BOOT-INF/lib/') -and $entry.FullName.EndsWith('.jar')) {
      $destination=Join-Path $libraries ([IO.Path]::GetFileName($entry.FullName))
      [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$destination,$true)
    }
  }
} finally { $archive.Dispose() }
& java '-Dfile.encoding=UTF-8' -cp (Join-Path $libraries '*') (Join-Path $PSScriptRoot 'SetAdminPassword.java')
if($LASTEXITCODE -ne 0){throw 'Administrator bootstrap did not complete.'}
