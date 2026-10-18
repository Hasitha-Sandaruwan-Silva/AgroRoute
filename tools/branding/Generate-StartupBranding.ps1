$ErrorActionPreference = 'Stop'
# Deterministic packaging of the official original, without tracing or generated art.
python (Join-Path $PSScriptRoot 'generate_official_branding.py')
if ($LASTEXITCODE -ne 0) { throw 'Official branding generation failed.' }
