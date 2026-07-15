param(
  [string]$Root = (Split-Path $PSScriptRoot -Parent)
)
$skip = @('.git', 'node_modules', '.venv', 'venv', '__pycache__', 'assets', 'target')
$ext = @('.java', '.js', '.jsx', '.ts', '.tsx', '.html', '.css', '.yml', '.yaml', '.tf', '.sh', '.sql', '.json', '.md', '.xml', '.env')
$total = 0
$files = 0
$byExt = @{}

Get-ChildItem -Path $Root -Recurse -File | ForEach-Object {
  $relParts = $_.FullName.Substring($Root.Length).Split([IO.Path]::DirectorySeparatorChar)
  foreach ($p in $relParts) {
    if ($skip -contains $p) { return }
  }
  $e = $_.Extension.ToLower()
  if ($e -notin $ext -and $_.Name -notin @('Dockerfile', 'docker-compose.yml', 'pom.xml')) { return }
  $n = (Get-Content -LiteralPath $_.FullName -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
  $total += $n
  $files++
  $key = if ($e) { $e } else { $_.Name }
  if (-not $byExt.ContainsKey($key)) { $byExt[$key] = 0 }
  $byExt[$key] += $n
}

Write-Output "files=$files loc=$total"
$byExt.GetEnumerator() | Sort-Object Value -Descending | ForEach-Object {
  Write-Output ("  {0}: {1}" -f $_.Key, $_.Value)
}
