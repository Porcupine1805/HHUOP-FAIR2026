param([string]$InputCsv = 'results\real_pilot\aggregate.csv', [string]$OutputCsv = 'results\real_pilot\summary.csv')
$ErrorActionPreference='Stop'
function Mean($xs){($xs|Measure-Object -Average).Average}
function Std($xs){if($xs.Count-lt 2){return 0};$m=Mean $xs;[Math]::Sqrt((($xs|ForEach-Object{($_-$m)*($_-$m)}|Measure-Object -Sum).Sum)/($xs.Count-1))}
$rows=Import-Csv $InputCsv
$summary=$rows|Group-Object dataset,alpha,beta,sensitiveFraction,algorithm|ForEach-Object{
 $r=$_.Group;$o=[ordered]@{dataset=$r[0].dataset;alpha=$r[0].alpha;beta=$r[0].beta;sensitiveFraction=$r[0].sensitiveFraction;algorithm=$r[0].algorithm;runs=$r.Count}
 foreach($metric in 'HF','MC','AC','DUS','utilityLoss','OD','TDR','QDR','actions','endToEndSanitizationMs','totalMs','peakMemoryMB'){$xs=@($r|ForEach-Object{[double]$_.$metric});$o["${metric}_mean"]=Mean $xs;$o["${metric}_std"]=Std $xs}
 [pscustomobject]$o
}
$summary|Export-Csv -NoTypeInformation -Encoding utf8 $OutputCsv
Write-Host "Wrote $($summary.Count) summary rows to $OutputCsv"
