# One-time setup for a locally installed SQL Server (no Docker).
# Creates the `shuttle` and `traccar` databases, their logins, the app schema and the sample data.
# Safe to run again: every step checks whether it already exists.
#
#   .\database\setup-local.ps1                              # default instance (localhost, port 1433)
#   .\database\setup-local.ps1 -Server "localhost\SQLEXPRESS"
#   .\database\setup-local.ps1 -Server "(localdb)\MSSQLLocalDB"   # browse in SSMS only; see README
#
# Connects with your Windows account (-E), so run it as a user who is a SQL Server sysadmin.
# Afterwards, open SSMS, connect to the same server, and expand Databases > shuttle.

param(
    [string]$Server = "localhost"
)

$ErrorActionPreference = "Stop"
$sqlcmd = (Get-Command sqlcmd -ErrorAction SilentlyContinue).Source
if (-not $sqlcmd) { throw "sqlcmd not found. It ships with SSMS / SQL Server; or install it from https://aka.ms/sqlcmd" }

$init = Join-Path $PSScriptRoot "init"
foreach ($file in Get-ChildItem $init -Filter *.sql | Sort-Object Name) {
    Write-Host "Running $($file.Name) on $Server"
    & $sqlcmd -S $Server -E -C -b -i $file.FullName
    if ($LASTEXITCODE -ne 0) { throw "$($file.Name) failed (exit $LASTEXITCODE)" }
}
Write-Host "Done. In SSMS connect to '$Server' (Windows Authentication) and open Databases > shuttle."
