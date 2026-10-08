# Code walkthrough source

The text and generator behind `docs/nlT-Structures-code-walkthrough.pdf`.

| File | What it is |
|---|---|
| `part1.md` … `part8.md` | The text: the intro plus step 1, then one file per step (2 to 8). |
| `template.html` | Page layout: cover, contents page, fonts, page numbers. |
| `BuildDoc.java` | Turns the combined Markdown into HTML and pulls in the code excerpts. |
| `build-pdf.ps1` | Does everything in one go and writes the PDF. |
| `Remap.java` | Shifts the line numbers in the text after the code changed (see below). |

## Rebuilding the PDF

From the project folder:

```
powershell -ExecutionPolicy Bypass -File docs\walkthrough\build-pdf.ps1
```

Needs Java 25 (`JAVA_HOME`, or the Temurin install) and Microsoft Edge. Temporary files go to `build\walkthrough\`. Close the PDF first if it's open in a viewer, otherwise it can't be overwritten.

## Writing in the .md files

- `#` starts a new step on a new page; `#` and `##` appear in the table of contents.
- Normal Markdown works: `###`, paragraphs, `- ` bullets (indent 2 spaces to nest), `1. ` lists, `|` tables, fenced code blocks, `` `code` ``, `**bold**`, `*italic*`, `---`.
- `@code <file> <from>-<to>` inserts those lines from the real source file, with their line numbers, e.g.
  `@code src/main/java/com/nltarzan/structures/util/Durations.java 9-47`.
  Several ranges: `11-15,40-52` (a ⋮ marks the gap).

The excerpts always show the **current** code, but the explanations under them name line numbers. After changing a file, check that its `@code` ranges and "Line …" references still match, then rebuild.

## Shifting line numbers after a code change

`Remap.java` fixes moved line numbers for you. It compares the code in the **previous** build (`build\walkthrough\walkthrough.html`) with the current source, and shifts every `@code` range and "Line …" reference to match. So run it **before** rebuilding, and not after a `gradlew clean`, which deletes that previous build. From `docs\walkthrough`:

```
java Remap.java ..\..\build\walkthrough\walkthrough.html ..\.. part1.md part2.md part3.md part4.md part5.md part6.md part7.md part8.md
```

It edits the `.md` files and prints every change. It only moves numbers: new or changed code still needs its explanation written by hand. Then rebuild with `build-pdf.ps1`.
