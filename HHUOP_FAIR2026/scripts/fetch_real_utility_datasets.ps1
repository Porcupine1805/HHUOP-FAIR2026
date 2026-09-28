$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$download = Join-Path $root 'build\downloads'
New-Item -ItemType Directory -Force -Path $download | Out-Null
& (Join-Path $PSScriptRoot 'build.bat')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$datasets = @{
  mushroom = 'https://www.philippe-fournier-viger.com/spmf/publicdatasets/mushroom_utility_spmf.txt'
  retail   = 'https://www.philippe-fournier-viger.com/spmf/publicdatasets/retail_utility_spmf.txt'
}
foreach ($name in $datasets.Keys) {
  $source = Join-Path $download ($name + '_utility_spmf.txt')
  & curl.exe -L --fail --retry 3 -o $source $datasets[$name]
  if ($LASTEXITCODE -ne 0) { throw "Download failed: $name" }
  java -cp (Join-Path $root 'build\hhuop-fair2026.jar') org.hhuop.experiment.SPMFUtilityConverterMain --input $source --outDir (Join-Path $root "datasets\$name")
  if ($LASTEXITCODE -ne 0) { throw "Conversion failed: $name" }
}
