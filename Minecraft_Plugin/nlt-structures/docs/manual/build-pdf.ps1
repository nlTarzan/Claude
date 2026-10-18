# Rebuilds docs\nlT-Structures-manual.pdf from manual.md.
# Run from anywhere:  powershell -ExecutionPolicy Bypass -File docs\manual\build-pdf.ps1

$ErrorActionPreference = "Stop"
$here = $PSScriptRoot
$project = (Resolve-Path "$here\..\..").Path
$work = "$project\build\manual"
$pdf = "$project\docs\nlT-Structures-manual.pdf"

# Java 25: JAVA_HOME if set, otherwise the Temurin install
$java = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" }
$edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"

# 1. Copy the text next to the template
New-Item -ItemType Directory -Force $work | Out-Null
Copy-Item "$here\manual.md" "$work\manual.md" -Force
Copy-Item "$here\template.html" "$work\template.html" -Force

# 2. Markdown -> HTML
& $java "$here\BuildDoc.java" "$work\manual.md" $project "$work\manual.html"
if ($LASTEXITCODE -ne 0) { throw "BuildDoc failed" }

# 3. HTML -> PDF with headless Edge
$url = "file:///" + "$work\manual.html".Replace('\', '/').Replace(' ', '%20')
Start-Process -FilePath $edge -Wait -WindowStyle Hidden -ArgumentList @(
    "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--print-to-pdf=`"$pdf`"", $url)

Write-Host "PDF written: $pdf ($((Get-Item $pdf).LastWriteTime))"
