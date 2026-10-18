# Manual source

The text and generator behind `docs/nlT-Structures-manual.pdf`.

| File | What it is |
|---|---|
| `manual.md` | The text of the manual. |
| `template.html` | Page layout: cover, contents page, fonts, page numbers. |
| `BuildDoc.java` | Turns the Markdown into HTML (the same generator as the code walkthrough in `docs/walkthrough` and the nlT-Races manual). |
| `build-pdf.ps1` | Does everything in one go and writes the PDF. |

## Rebuilding the PDF

From the project folder:

```
powershell -ExecutionPolicy Bypass -File docs\manual\build-pdf.ps1
```

Needs Java 25 (`JAVA_HOME`, or the Temurin install) and Microsoft Edge. Temporary files go to `build\manual\`. Close the PDF first if it's open in a viewer, otherwise it can't be overwritten.

## Writing in manual.md

- `#` starts a section and `##` a subsection; both appear in the table of contents.
- Supported Markdown: `###`, paragraphs, `- ` bullets (indent 2 spaces to nest), `1. ` lists, `|` tables, fenced code blocks, `` `code` ``, `**bold**`, `*italic*`.
- Not supported: links (`[text](url)`) and `>` quotes. Refer to other parts as "section 8 (All settings)".

Update the manual whenever commands, settings or messages change, then rebuild.
