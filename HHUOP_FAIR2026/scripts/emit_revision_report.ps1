$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$rev = Join-Path $root "results\revision"
$out = Join-Path $root "docs\REVISION_RESULTS.md"
function Table($path, $title) {
  if (-not (Test-Path -LiteralPath $path)) { return "### $title`n`nNo file: $path`n" }
  $rows = Import-Csv -LiteralPath $path
  $sb = New-Object System.Text.StringBuilder
  [void]$sb.AppendLine("### $title")
  [void]$sb.AppendLine("")
  [void]$sb.AppendLine("Rows: $($rows.Count)")
  [void]$sb.AppendLine("")
  $show = $rows | Select-Object -First 40
  if ($show.Count -gt 0) {
    $cols = $show[0].PSObject.Properties.Name
    [void]$sb.AppendLine("| " + ($cols -join " | ") + " |")
    [void]$sb.AppendLine("| " + (($cols | ForEach-Object { "---" }) -join " | ") + " |")
    foreach ($r in $show) {
      $vals = foreach ($c in $cols) { ($r.$c -replace '\|','/') }
      [void]$sb.AppendLine("| " + ($vals -join " | ") + " |")
    }
    if ($rows.Count -gt 40) { [void]$sb.AppendLine("") ; [void]$sb.AppendLine("Showing 40 of $($rows.Count) rows.") }
  }
  [void]$sb.AppendLine("")
  return $sb.ToString()
}
$md = @(
  "# Revision experiment results",
  "",
  "Generated from ``results/revision``. See ``docs/REVIEWER_EXPERIMENT_PROTOCOL.md`` for the design.",
  "",
  (Get-Content -LiteralPath (Join-Path $rev "GRID_OK") -ErrorAction SilentlyContinue | Out-String),
  (Table (Join-Path $rev "e1_stats\summary_mean_ci.csv") "E1 means and 95 percent t intervals"),
  (Table (Join-Path $rev "e1_stats\friedman.csv") "E1 Friedman"),
  (Table (Join-Path $rev "e1_stats\pairwise_holm.csv") "E1 pairwise Holm"),
  (Table (Join-Path $rev "e2_stats\friedman.csv") "E2 Friedman"),
  (Table (Join-Path $rev "e2_stats\summary_mean_ci.csv") "E2 means"),
  (Table (Join-Path $rev "e3_wM\summary_mean_ci.csv") "E3 wM"),
  (Table (Join-Path $rev "e3_wA\summary_mean_ci.csv") "E3 wA"),
  (Table (Join-Path $rev "e3_wD\summary_mean_ci.csv") "E3 wD"),
  (Table (Join-Path $rev "e3_wS\summary_mean_ci.csv") "E3 wS"),
  (Table (Join-Path $rev "e4_stats\summary_mean_ci.csv") "E4 utility realizations")
) -join "`n"
Set-Content -LiteralPath $out -Value $md -Encoding utf8
Write-Output "Wrote $out"
