# Builds nothing: copies the newest built jars into the Powers profile and Downloads, old ones to old-mods.
# powershell -File tools/install.ps1 <charpowers version> [powerguide version]
param([string]$Version, [string]$Guide)
$mods = "$env:APPDATA\ModrinthApp\profiles\Powers\mods"
$old = "$env:USERPROFILE\Downloads\old-mods"
$dl = "$env:USERPROFILE\Downloads"
function Put($name, $version, $built) {
    if (-not (Test-Path $built)) { throw "No jar at $built" }
    Get-ChildItem $mods -Filter "{C} $name-*.jar" | Where-Object { $_.Name -ne "{C} $name-$version.jar" } | ForEach-Object { Move-Item -LiteralPath $_.FullName -Destination $old -Force }
    Get-ChildItem $dl -Filter "{C} $name-*.jar" | Where-Object { $_.Name -ne "{C} $name-$version.jar" } | ForEach-Object { Move-Item -LiteralPath $_.FullName -Destination $old -Force }
    Copy-Item -LiteralPath $built -Destination (Join-Path $mods "{C} $name-$version.jar") -Force
    Copy-Item -LiteralPath $built -Destination (Join-Path $dl "{C} $name-$version.jar") -Force
    "{C} $name-$version.jar: " + (Get-Item -LiteralPath $built).Length + " bytes"
}
Put "charpowers" $Version "$dl\charpowers\build\libs\charpowers-$Version.jar"
if ($Guide) { Put "powerguide" $Guide "$dl\powerguide\mod\build\libs\powerguide-$Guide.jar" }
