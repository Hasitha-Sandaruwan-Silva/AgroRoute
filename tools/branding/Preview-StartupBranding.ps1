$ErrorActionPreference = 'Stop'
# Previews are rendered from the same official raster assets as the launcher.
& (Join-Path $PSScriptRoot 'Generate-StartupBranding.ps1')
Write-Output 'Preview: delivery/official-branding/launcher-masks.png'
