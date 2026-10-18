import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns walkthrough.md (a small Markdown dialect) into walkthrough.html.
 * Usage: java BuildDoc.java <source.md> <project root> <output.html>
 *
 * Supported: # / ## / ### headings, paragraphs, "- " bullets (nested by 2 spaces), "1. " numbered
 * lists, | tables |, ``` fenced blocks ```, ---, inline `code` **bold** *italic*, and
 *   @code <path> <from>-<to>[,<from>-<to>...]   a numbered excerpt read from the real source file.
 */
public class BuildDoc {

    private static Path root;
    private static final StringBuilder toc = new StringBuilder();
    private static int anchor;

    public static void main(String[] args) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
        root = Path.of(args[1]);
        StringBuilder body = new StringBuilder();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.strip();
            if (trimmed.isEmpty()) {
                i++;
            } else if (trimmed.startsWith("```")) {
                StringBuilder block = new StringBuilder();
                i++;
                while (i < lines.size() && !lines.get(i).strip().startsWith("```")) {
                    block.append(esc(lines.get(i))).append('\n');
                    i++;
                }
                i++;
                body.append("<pre class=\"plain\">").append(block).append("</pre>\n");
            } else if (trimmed.startsWith("@code ")) {
                body.append(code(trimmed.substring(6).strip()));
                i++;
            } else if (trimmed.equals("---")) {
                body.append("<hr>\n");
                i++;
            } else if (trimmed.startsWith("### ")) {
                body.append("<h3>").append(inline(trimmed.substring(4))).append("</h3>\n");
                i++;
            } else if (trimmed.startsWith("## ")) {
                String id = "s" + (++anchor);
                body.append("<h2 id=\"").append(id).append("\">").append(inline(trimmed.substring(3))).append("</h2>\n");
                toc.append("<li class=\"sub\"><a href=\"#").append(id).append("\">").append(inline(trimmed.substring(3))).append("</a></li>\n");
                i++;
            } else if (trimmed.startsWith("# ")) {
                String id = "s" + (++anchor);
                body.append("<h1 id=\"").append(id).append("\">").append(inline(trimmed.substring(2))).append("</h1>\n");
                toc.append("<li class=\"main\"><a href=\"#").append(id).append("\">").append(inline(trimmed.substring(2))).append("</a></li>\n");
                i++;
            } else if (trimmed.startsWith("|")) {
                List<String> rows = new ArrayList<>();
                while (i < lines.size() && lines.get(i).strip().startsWith("|")) {
                    rows.add(lines.get(i).strip());
                    i++;
                }
                body.append(table(rows));
            } else if (isListItem(line)) {
                List<String> items = new ArrayList<>();
                while (i < lines.size() && isListItem(lines.get(i))) {
                    items.add(lines.get(i));
                    i++;
                }
                body.append(list(items, 0, new int[]{0}));
            } else {
                StringBuilder para = new StringBuilder(trimmed);
                i++;
                while (i < lines.size() && !lines.get(i).isBlank() && !isBlockStart(lines.get(i))) {
                    para.append(' ').append(lines.get(i).strip());
                    i++;
                }
                body.append("<p>").append(inline(para.toString())).append("</p>\n");
            }
        }

        String template = Files.readString(Path.of(args[0]).resolveSibling("template.html"), StandardCharsets.UTF_8);
        String html = template.replace("{{TOC}}", toc.toString()).replace("{{BODY}}", body.toString());
        Files.writeString(Path.of(args[2]), html, StandardCharsets.UTF_8);
        System.out.println("Wrote " + args[2]);
    }

    private static boolean isBlockStart(String line) {
        String t = line.strip();
        return t.startsWith("#") || t.startsWith("|") || t.startsWith("```") || t.startsWith("@code ")
                || t.equals("---") || isListItem(line);
    }

    private static final Pattern LIST_ITEM = Pattern.compile("^( *)(- |\\d+\\. )(.*)$");

    private static boolean isListItem(String line) {
        return LIST_ITEM.matcher(line).matches();
    }

    /** Builds nested lists from "- " / "1. " lines indented by 2 spaces per level. */
    private static String list(List<String> items, int depth, int[] pos) {
        StringBuilder out = new StringBuilder();
        Matcher first = LIST_ITEM.matcher(items.get(pos[0]));
        first.matches();
        boolean ordered = !first.group(2).startsWith("-");
        out.append(ordered ? "<ol>" : "<ul>");
        while (pos[0] < items.size()) {
            Matcher m = LIST_ITEM.matcher(items.get(pos[0]));
            m.matches();
            int level = m.group(1).length() / 2;
            if (level < depth) {
                break;
            }
            if (level > depth) {
                out.append(list(items, level, pos));
                continue;
            }
            out.append("<li>").append(inline(m.group(3)));
            pos[0]++;
            if (pos[0] < items.size()) {
                Matcher next = LIST_ITEM.matcher(items.get(pos[0]));
                next.matches();
                if (next.group(1).length() / 2 > depth) {
                    out.append(list(items, next.group(1).length() / 2, pos));
                }
            }
            out.append("</li>");
        }
        out.append(ordered ? "</ol>\n" : "</ul>\n");
        return out.toString();
    }

    private static String table(List<String> rows) {
        StringBuilder out = new StringBuilder("<table>\n");
        boolean header = rows.size() > 1 && rows.get(1).matches("\\|[\\s:|-]+\\|?");
        for (int r = 0; r < rows.size(); r++) {
            if (header && r == 1) {
                continue;
            }
            String row = rows.get(r);
            String inner = row.substring(1, row.endsWith("|") ? row.length() - 1 : row.length());
            String[] cells = inner.split("(?<!\\\\)\\|", -1);
            String tag = header && r == 0 ? "th" : "td";
            out.append("<tr>");
            for (String cell : cells) {
                out.append('<').append(tag).append('>').append(inline(cell.strip().replace("\\|", "|")))
                        .append("</").append(tag).append('>');
            }
            out.append("</tr>\n");
        }
        return out.append("</table>\n").toString();
    }

    /** "@code path 10-20,25-30": numbered lines from the real file. */
    private static String code(String spec) throws IOException {
        int space = spec.lastIndexOf(' ');
        String path = spec.substring(0, space);
        String ranges = spec.substring(space + 1);
        List<String> file = Files.readAllLines(root.resolve(path), StandardCharsets.UTF_8);
        String name = Path.of(path).getFileName().toString();
        StringBuilder out = new StringBuilder();
        String[] parts = ranges.split(",");
        int total = 0;
        for (String part : parts) {
            String[] fromTo = part.split("-");
            total += Integer.parseInt(fromTo[1]) - Integer.parseInt(fromTo[0]) + 1;
        }
        out.append("<div class=\"code").append(total <= 30 ? " keep" : "").append("\"><div class=\"file\">")
                .append(esc(name)).append("</div><pre>");
        for (int p = 0; p < parts.length; p++) {
            String[] fromTo = parts[p].split("-");
            int from = Integer.parseInt(fromTo[0]);
            int to = Integer.parseInt(fromTo[1]);
            if (to > file.size()) {
                throw new IllegalArgumentException(path + " has only " + file.size() + " lines, asked for " + to);
            }
            if (p > 0) {
                out.append("<span class=\"gap\">      ⋮</span>\n");
            }
            for (int n = from; n <= to; n++) {
                out.append("<span class=\"ln\">").append(String.format("%4d", n)).append("</span>  ")
                        .append(esc(file.get(n - 1))).append('\n');
            }
        }
        return out.append("</pre></div>\n").toString();
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static final Pattern CODE = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])");

    private static String inline(String text) {
        // Protect inline code first so ** and * inside it stay literal.
        List<String> codes = new ArrayList<>();
        Matcher m = CODE.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            codes.add("<code>" + esc(m.group(1)) + "</code>");
            m.appendReplacement(sb, "\u0000" + (codes.size() - 1) + "\u0000");
        }
        m.appendTail(sb);
        String out = esc(sb.toString());
        out = BOLD.matcher(out).replaceAll("<strong>$1</strong>");
        out = ITALIC.matcher(out).replaceAll("<em>$1</em>");
        for (int c = 0; c < codes.size(); c++) {
            out = out.replace("\u0000" + c + "\u0000", codes.get(c));
        }
        return out;
    }
}
