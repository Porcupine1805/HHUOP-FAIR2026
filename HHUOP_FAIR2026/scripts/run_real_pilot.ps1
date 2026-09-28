$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
& (Join-Path $PSScriptRoot 'build.bat'); if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$outRoot = Join-Path $root 'results\real_pilot'; New-Item -ItemType Directory -Force -Path $outRoot | Out-Null
$configs = @(
  @{d='mushroom'; a='0.20';  b='0.55'; f='0.001'},
  @{d='mushroom'; a='0.20';  b='0.60'; f='0.001'},
  @{d='retail';   a='0.005'; b='0.20'; f='0.05'},
  @{d='retail';   a='0.005'; b='0.30'; f='0.05'}
)
foreach($c in $configs){foreach($seed in 2026,2027,2028){
  $tag="$($c.d)_a$($c.a)_b$($c.b)_f$($c.f)_s$seed"
  java -Xms512m -Xmx6g -cp (Join-Path $root 'build\hhuop-fair2026.jar') org.hhuop.experiment.ExperimentMain --db (Join-Path $root "datasets\$($c.d)\database.txt") --profits (Join-Path $root "datasets\$($c.d)\profits.txt") --alpha $c.a --beta $c.b --sensitiveFraction $c.f --seed $seed --out (Join-Path $outRoot "$tag.csv") --actionDir (Join-Path $outRoot "${tag}_actions")
  if($LASTEXITCODE -ne 0){throw "Benchmark failed: $tag"}
}}
java -cp (Join-Path $root 'build\hhuop-fair2026.jar') org.hhuop.experiment.AggregateResultsMain --inputDir $outRoot --out (Join-Path $outRoot 'aggregate.csv')
