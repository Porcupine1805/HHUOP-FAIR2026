$ErrorActionPreference = "Continue"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $root
$jdkBin = "C:\Users\tranc\AppData\Local\Temp\jdk17\jdk-17.0.20.1+1\bin"
if (Test-Path -LiteralPath (Join-Path $jdkBin "java.exe")) { $env:PATH = "$jdkBin;" + $env:PATH }
$java = (Get-Command java).Source
$outRoot = Join-Path $root "results\revision"
New-Item -ItemType Directory -Force -Path $outRoot | Out-Null
Set-Content -LiteralPath (Join-Path $outRoot "grid.pid") -Value $PID -Encoding ascii
$progress = Join-Path $outRoot "progress.log"
$failLog = Join-Path $outRoot "failures.txt"
if (Test-Path -LiteralPath (Join-Path $outRoot "GRID_OK")) { Remove-Item -LiteralPath (Join-Path $outRoot "GRID_OK") }

$jobs = New-Object System.Collections.Generic.List[object]
function Add-Job($scenario, $dataset, $db, $profits, $alpha, $beta, $fraction, $seed, $alg, $wM, $wA, $wD, $wS) {
  $script:jobs.Add([pscustomobject]@{
    scenario=$scenario; dataset=$dataset; db=$db; profits=$profits; alpha=$alpha; beta=$beta
    fraction=$fraction; seed=$seed; alg=$alg; wM=$wM; wA=$wA; wD=$wD; wS=$wS
  }) | Out-Null
}
$algs = @("SMAU","SMIU","SMSE","DEL","HUI","GRED")
$seeds10 = 2026..2035
$seeds5 = 2026..2030
$fractions = @("0.05","0.10","0.25")
$primary = @(
  @{n="retail"; a="0.005"; b="0.30"},
  @{n="mushroom"; a="0.20"; b="0.65"},
  @{n="foodmart"; a="0.003"; b="0.30"},
  @{n="chess"; a="0.62"; b="0.40"}
)
$thresholds = @(
  @{n="chess"; a="0.62"; b="0.44"},
  @{n="chess"; a="0.62"; b="0.46"},
  @{n="mushroom"; a="0.20"; b="0.60"},
  @{n="retail"; a="0.005"; b="0.25"},
  @{n="foodmart"; a="0.003"; b="0.35"},
  @{n="foodmart"; a="0.003"; b="0.40"}
)
$weights = @(
  @{m="1"; a="1"; d="0.1"; s="0.5"},
  @{m="0"; a="1"; d="0.1"; s="0.5"},
  @{m="0.5"; a="1"; d="0.1"; s="0.5"},
  @{m="2"; a="1"; d="0.1"; s="0.5"},
  @{m="5"; a="1"; d="0.1"; s="0.5"},
  @{m="1"; a="0"; d="0.1"; s="0.5"},
  @{m="1"; a="0.5"; d="0.1"; s="0.5"},
  @{m="1"; a="2"; d="0.1"; s="0.5"},
  @{m="1"; a="5"; d="0.1"; s="0.5"},
  @{m="1"; a="1"; d="0"; s="0.5"},
  @{m="1"; a="1"; d="0.5"; s="0.5"},
  @{m="1"; a="1"; d="1"; s="0.5"},
  @{m="1"; a="1"; d="0.1"; s="0"},
  @{m="1"; a="1"; d="0.1"; s="1"},
  @{m="1"; a="1"; d="0.1"; s="2"}
)
foreach ($d in $primary) {
  $db = Join-Path $root "datasets\$($d.n)\database.txt"
  $pf = Join-Path $root "datasets\$($d.n)\profits.txt"
  foreach ($f in $fractions) { foreach ($s in $seeds10) { foreach ($alg in $algs) {
    Add-Job "e1" $d.n $db $pf $d.a $d.b $f $s $alg "1" "1" "0.1" "0.5"
  }}}
  foreach ($s in $seeds10) {
    Add-Job "e3" $d.n $db $pf $d.a $d.b "0.10" $s "SMSE" "1" "1" "0.1" "0.5"
  }
  foreach ($w in $weights) { if ($w.m -eq "1" -and $w.a -eq "1" -and $w.d -eq "0.1" -and $w.s -eq "0.5") { continue }
    foreach ($s in $seeds5) { Add-Job "e3" $d.n $db $pf $d.a $d.b "0.10" $s "SMSE" $w.m $w.a $w.d $w.s }
  }
}
foreach ($d in $thresholds) {
  $db = Join-Path $root "datasets\$($d.n)\database.txt"
  $pf = Join-Path $root "datasets\$($d.n)\profits.txt"
  foreach ($s in $seeds10) { foreach ($alg in $algs) { Add-Job "e2" $d.n $db $pf $d.a $d.b "0.10" $s $alg "1" "1" "0.1" "0.5" } }
}
foreach ($base in @("chess","mushroom","retail")) {
  $alpha = if ($base -eq "chess") { "0.62" } elseif ($base -eq "mushroom") { "0.20" } else { "0.005" }
  $beta = if ($base -eq "retail") { "0.30" } elseif ($base -eq "mushroom") { "0.65" } else { "0.40" }
  foreach ($u in 701..705) {
    $name = "${base}_u$u"
    $db = Join-Path $root "datasets\realizations\$name\database.txt"
    $pf = Join-Path $root "datasets\realizations\$name\profits.txt"
    foreach ($s in $seeds5) { foreach ($alg in $algs) { Add-Job "e4" $name $db $pf $alpha $beta "0.10" $s $alg "1" "1" "0.1" "0.5" } }
  }
}

$throttle = 4
$done = 0; $failed = 0; $skipped = 0
Add-Content -LiteralPath $progress -Value ("START jobs={0} parallel={1} {2}" -f $jobs.Count, $throttle, (Get-Date -Format o))
$pending = New-Object System.Collections.Generic.List[object]
foreach ($job in $jobs) {
  $dir = Join-Path $outRoot $job.scenario
  New-Item -ItemType Directory -Force -Path $dir | Out-Null
  $tag = "{0}_a{1}_b{2}_f{3}_s{4}_{5}" -f $job.dataset, $job.alpha, $job.beta, $job.fraction, $job.seed, $job.alg
  if ($job.scenario -eq "e3") { $tag += "_wM$($job.wM)_wA$($job.wA)_wD$($job.wD)_wS$($job.wS)" }
  $out = Join-Path $dir "$tag.csv"
  $rssPath = [regex]::Replace($out, "\.csv$", ".rss")
  if (Test-Path -LiteralPath $out) {
    $lines = @(Get-Content -LiteralPath $out)
    if ($lines.Count -ge 2 -and $lines[1] -match 'HHUOP-') { $skipped++; continue }
  }
  $pending.Add([pscustomobject]@{ Tag=$tag; Out=$out; RssPath=$rssPath; Job=$job; Dir=$dir }) | Out-Null
}
$script:running = @()
function Harvest-Running {
  $still = @()
  foreach ($r in @($script:running)) {
    if (-not $r.TimedOut -and -not $r.Proc.HasExited -and (Get-Date) -gt $r.Deadline) {
      try { Stop-Process -Id $r.Proc.Id -Force -ErrorAction SilentlyContinue } catch {}
      Add-Content -LiteralPath $failLog -Value "TIMEOUT $($r.Out)"
      Add-Content -LiteralPath $progress -Value "FAIL timeout $($r.Tag)"
      $script:failed++; $r.TimedOut = $true
    }
    if (-not $r.Proc.HasExited) {
      try { $r.Proc.Refresh(); if ($r.Proc.PeakWorkingSet64 -gt $r.Peak) { $r.Peak = $r.Proc.PeakWorkingSet64 } } catch {}
      $still += $r
      continue
    }
    if ($r.TimedOut) { continue }
    try { $null = $r.Proc.WaitForExit(); $r.Proc.Refresh(); if ($r.Proc.PeakWorkingSet64 -gt $r.Peak) { $r.Peak = $r.Proc.PeakWorkingSet64 } } catch {}
    Set-Content -LiteralPath $r.RssPath -Value ([math]::Round($r.Peak / 1MB, 3)) -Encoding ascii
    $code = $r.Proc.ExitCode
    $okFile = (Test-Path -LiteralPath $r.Out) -and (@(Get-Content -LiteralPath $r.Out -ErrorAction SilentlyContinue).Count -ge 2)
    if (($null -ne $code -and $code -ne 0) -or -not $okFile) {
      Add-Content -LiteralPath $failLog -Value "EXIT $code $($r.Out)"
      Add-Content -LiteralPath $progress -Value "FAIL exit=$code $($r.Tag)"
      $script:failed++
    } else {
      $script:done++
      if ((($script:done + $script:skipped) % 25) -eq 0) {
        Add-Content -LiteralPath $progress -Value ("OK done={0} skipped={1} failed={2} last={3}" -f $script:done, $script:skipped, $script:failed, $r.Tag)
      }
    }
  }
  $script:running = $still
}
foreach ($item in $pending) {
  while ($script:running.Count -ge $throttle) { Harvest-Running; if ($script:running.Count -ge $throttle) { Start-Sleep -Milliseconds 300 } }
  $actionDir = Join-Path $item.Dir ("actions_" + [IO.Path]::GetFileNameWithoutExtension($item.Out))
  $job = $item.Job
  $argList = @(
    "-Xms256m","-Xmx4g","-cp",(Join-Path $root "build\hhuop-fair2026.jar"),
    "org.hhuop.experiment.ExperimentMain",
    "--db",$job.db,"--profits",$job.profits,
    "--alpha",$job.alpha,"--beta",$job.beta,
    "--sensitiveFraction",$job.fraction,"--seed",[string]$job.seed,
    "--algorithms",$job.alg,
    "--wM",$job.wM,"--wA",$job.wA,"--wD",$job.wD,"--wS",$job.wS,
    "--out",$item.Out,"--actionDir",$actionDir
  )
  $quoted = foreach ($arg in $argList) { if ($arg -match '[\s"]') { '"' + ($arg -replace '"','`"') + '"' } else { $arg } }
  $p = Start-Process -FilePath $java -ArgumentList ($quoted -join ' ') -PassThru -WindowStyle Hidden -RedirectStandardOutput ($item.Out + ".stdout") -RedirectStandardError ($item.Out + ".stderr") -WorkingDirectory $root
  $script:running += [pscustomobject]@{ Proc=$p; Peak=0L; Deadline=(Get-Date).AddMinutes(20); Out=$item.Out; RssPath=$item.RssPath; Tag=$item.Tag; TimedOut=$false }
}
while ($script:running.Count -gt 0) { Harvest-Running; if ($script:running.Count -gt 0) { Start-Sleep -Milliseconds 300 } }
Add-Content -LiteralPath $progress -Value ("FINISHED done={0} skipped={1} failed={2} {3}" -f $done, $skipped, $failed, (Get-Date -Format o))
function Invoke-Stats($scenario, $factor, $hold) {
  $in = Join-Path $outRoot $scenario
  $stats = Join-Path $outRoot ($scenario + "_stats")
  if ($hold) {
    & $java -cp (Join-Path $root "build\hhuop-fair2026.jar") org.hhuop.experiment.RevisionStatsMain --inputDir $in --outDir $stats --factor $factor --hold $hold
  } else {
    & $java -cp (Join-Path $root "build\hhuop-fair2026.jar") org.hhuop.experiment.RevisionStatsMain --inputDir $in --outDir $stats --factor $factor
  }
}
Invoke-Stats "e1" "algorithm" ""
Invoke-Stats "e2" "algorithm" ""
Invoke-Stats "e4" "algorithm" ""
Invoke-Stats "e3" "wM" "algorithm=HHUOP-SMSE,wA=1,wD=0.1,wS=0.5"
Invoke-Stats "e3" "wA" "algorithm=HHUOP-SMSE,wM=1,wD=0.1,wS=0.5"
# The three calls above overwrite e3_stats. Write each factor to its own directory.
& $java -cp (Join-Path $root "build\hhuop-fair2026.jar") org.hhuop.experiment.RevisionStatsMain --inputDir (Join-Path $outRoot "e3") --outDir (Join-Path $outRoot "e3_wM") --factor wM --hold "algorithm=HHUOP-SMSE,wA=1,wD=0.1,wS=0.5"
& $java -cp (Join-Path $root "build\hhuop-fair2026.jar") org.hhuop.experiment.RevisionStatsMain --inputDir (Join-Path $outRoot "e3") --outDir (Join-Path $outRoot "e3_wA") --factor wA --hold "algorithm=HHUOP-SMSE,wM=1,wD=0.1,wS=0.5"
& $java -cp (Join-Path $root "build\hhuop-fair2026.jar") org.hhuop.experiment.RevisionStatsMain --inputDir (Join-Path $outRoot "e3") --outDir (Join-Path $outRoot "e3_wD") --factor wD --hold "algorithm=HHUOP-SMSE,wM=1,wA=1,wS=0.5"
& $java -cp (Join-Path $root "build\hhuop-fair2026.jar") org.hhuop.experiment.RevisionStatsMain --inputDir (Join-Path $outRoot "e3") --outDir (Join-Path $outRoot "e3_wS") --factor wS --hold "algorithm=HHUOP-SMSE,wM=1,wA=1,wD=0.1"
Set-Content -LiteralPath (Join-Path $outRoot "GRID_OK") -Value ("done={0} skipped={1} failed={2}" -f $done, $skipped, $failed) -Encoding ascii
Add-Content -LiteralPath $progress -Value "GRID_OK"
exit 0
