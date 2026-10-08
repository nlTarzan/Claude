# Rebuilds docs\nlT-Structures-code-walkthrough.pdf from part1.md .. part8.md.
# Run from anywhere:  powershell -ExecutionPolicy Bypass -File docs\walkthrough\build-pdf.ps1
# Code excerpts (@code lines) are read from the current source files, so update their
# line ranges in the .md files whenever the code they point to moves.

$ErrorActionPreference = "Stop"
$here = $PSScriptRoot
$project = (Resolve-Path "$here\..\..").Path
$work = "$project\build\walkthrough"
$pdf = "$project\docs\nlT-Structures-code-walkthrough.pdf"

# Java 25: JAVA_HOME if set, otherwise the Temurin install
$java = if ($env:JAVA_HOME) { "$env:JAVA_HOME\bin\java.exe" } else { "C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot\bin\java.exe" }
$edge = "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"

# 1. Combine the parts (in order) next to a copy of the template
New-Item -ItemType Directory -Force $work | Out-Null
$parts = Get-ChildItem "$here\part*.md" | Sort-Object { [int]($_.BaseName -replace '\D', '') }
$text = ($parts | ForEach-Object { [IO.File]::ReadAllText($_.FullName) }) -join "`n"
[IO.File]::WriteAllText("$work\walkthrough.md", $text, (New-Object Text.UTF8Encoding $false))
Copy-Item "$here\template.html" "$work\template.html" -Force

# 2. Markdown + source excerpts -> HTML
& $java "$here\BuildDoc.java" "$work\walkthrough.md" $project "$work\walkthrough.html"
if ($LASTEXITCODE -ne 0) { throw "BuildDoc failed" }

# 3. HTML -> PDF with headless Edge
$url = "file:///" + "$work\walkthrough.html".Replace('\', '/').Replace(' ', '%20')
Start-Process -FilePath $edge -Wait -WindowStyle Hidden -ArgumentList @(
    "--headless=new", "--disable-gpu", "--no-pdf-header-footer", "--print-to-pdf=`"$pdf`"", $url)

Write-Host "PDF written: $pdf ($((Get-Item $pdf).LastWriteTime))"
