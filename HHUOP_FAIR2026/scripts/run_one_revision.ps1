param(
  [Parameter(Mandatory=$true)][string]$Db,
  [Parameter(Mandatory=$true)][string]$Profits,
  [Parameter(Mandatory=$true)][string]$Alpha,
  [Parameter(Mandatory=$true)][string]$Beta,
  [Parameter(Mandatory=$true)][string]$Fraction,
  [Parameter(Mandatory=$true)][string]$Seed,
  [Parameter(Mandatory=$true)][string]$Algorithm,
  [Parameter(Mandatory=$true)][string]$Out,
  [string]$WM = "1",
  [string]$WA = "1",
  [string]$WD = "0.1",
  [string]$WS = "0.5",
  [int]$MaxActions = 100000
)
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$java = (Get-Command java).Source
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Out) | Out-Null
$actionDir = Join-Path (Split-Path -Parent $Out) ("actions_" + [IO.Path]::GetFileNameWithoutExtension($Out))
if ((Test-Path -LiteralPath $Out) -and (Test-Path -LiteralPath ($Out -replace '\.csv$','.rss'))) {
  $existing = @(Get-Content -LiteralPath $Out)
  if ($existing.Count -ge 2) { Write-Output "SKIP $Out"; exit 0 }
}
$argList = @(
  "-Xms512m","-Xmx6g","-cp",(Join-Path $root "build\hhuop-fair2026.jar"),
  "org.hhuop.experiment.ExperimentMain",
  "--db",$Db,"--profits",$Profits,
  "--alpha",$Alpha,"--beta",$Beta,
  "--sensitiveFraction",$Fraction,"--seed",$Seed,
  "--algorithms",$Algorithm,
  "--wM",$WM,"--wA",$WA,"--wD",$WD,"--wS",$WS,
  "--maxActions",[string]$MaxActions,
  "--out",$Out,"--actionDir",$actionDir
)
$stdout = $Out + ".stdout"
$stderr = $Out + ".stderr"
$p = Start-Process -FilePath $java -ArgumentList $argList -PassThru -NoNewWindow -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WorkingDirectory $root
$peak = 0L
while (-not $p.HasExited) {
  try { $p.Refresh(); if ($p.PeakWorkingSet64 -gt $peak) { $peak = $p.PeakWorkingSet64 } } catch {}
  Start-Sleep -Milliseconds 100
}
try { $p.Refresh(); if ($p.PeakWorkingSet64 -gt $peak) { $peak = $p.PeakWorkingSet64 } } catch {}
$rss = [math]::Round($peak / 1MB, 3)
Set-Content -LiteralPath ($Out -replace '\.csv$','.rss') -Value $rss -Encoding ascii
if ($p.ExitCode -ne 0) {
  Write-Output "FAIL exit=$($p.ExitCode) rssMB=$rss $Out"
  exit $p.ExitCode
}
Write-Output "OK rssMB=$rss $Out"
