import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * java Remap.java <old walkthrough.html> <project root> <part.md>...
 * Shifts @code ranges and "Line(s) N" references in the parts from the old source lines (taken
 * from the old HTML's excerpts) to the current source. Prints every change it makes.
 */
public class Remap {
    static Map<String, Path> fileByName = new HashMap<>();
    static Map<String, TreeMap<Integer, Integer>> maps = new HashMap<>();

    public static void main(String[] a) throws Exception {
        String html = Files.readString(Path.of(a[0]));
        Path root = Path.of(a[1]);
        Map<String, TreeMap<Integer, String>> old = new HashMap<>();
        Matcher m = Pattern.compile("<div class=\"file\">([^<]*)</div><pre>(.*?)</pre>", Pattern.DOTALL).matcher(html);
        Pattern ln = Pattern.compile("^<span class=\"ln\">\\s*(\\d+)</span>  (.*)$");
        while (m.find()) {
            TreeMap<Integer, String> lines = old.computeIfAbsent(m.group(1), k -> new TreeMap<>());
            for (String l : m.group(2).split("\n")) {
                Matcher lm = ln.matcher(l);
                if (lm.find()) {
                    lines.put(Integer.parseInt(lm.group(1)), unescape(lm.group(2).replaceAll("<[^>]*>", "")));
                }
            }
        }
        try (var walk = Files.walk(root.resolve("src"))) {
            walk.filter(Files::isRegularFile).forEach(p -> fileByName.put(p.getFileName().toString(), p));
        }
        for (String f : List.of("build.gradle.kts", "settings.gradle.kts")) {
            fileByName.put(f, root.resolve(f));
        }
        for (var e : old.entrySet()) {
            List<String> now = Files.readAllLines(fileByName.get(e.getKey()), StandardCharsets.UTF_8);
            TreeMap<Integer, Integer> map = new TreeMap<>();
            int pos = 0; // 0-based index in the new file to search from
            for (var l : e.getValue().entrySet()) {
                int guess = l.getKey() - 1 + (map.isEmpty() ? 0 : map.lastEntry().getValue() - map.lastKey());
                int found = -1;
                for (int i = Math.max(pos, guess - 2); i < Math.min(now.size(), Math.max(pos, guess) + 25); i++) {
                    if (now.get(i).equals(l.getValue())) { found = i; break; }
                }
                if (found >= 0) {
                    map.put(l.getKey(), found + 1);
                    pos = found + 1;
                }
            }
            maps.put(e.getKey(), map);
        }

        Pattern codeLine = Pattern.compile("^@code (\\S+) (.+)$");
        Pattern heading = Pattern.compile("^##+ (\\S+\\.(?:java|yml|kts))");
        Pattern ref = Pattern.compile("\\b([Ll]ines?) (\\d+(?:[–-]\\d+)?(?:(?:, | and |–)\\d+(?:[–-]\\d+)?)*)");
        for (int i = 2; i < a.length; i++) {
            Path md = Path.of(a[i]);
            List<String> out = new ArrayList<>();
            String file = null;
            int n = 0;
            for (String line : Files.readAllLines(md, StandardCharsets.UTF_8)) {
                n++;
                String orig = line;
                Matcher cm = codeLine.matcher(line);
                Matcher hm = heading.matcher(line);
                if (cm.find()) {
                    file = Path.of(cm.group(1)).getFileName().toString();
                    line = "@code " + cm.group(1) + " " + shiftNumbers(file, cm.group(2));
                } else {
                    if (hm.find()) {
                        file = hm.group(1);
                    }
                    if (file != null) {
                        StringBuilder sb = new StringBuilder();
                        Matcher rm = ref.matcher(line);
                        while (rm.find()) {
                            rm.appendReplacement(sb, Matcher.quoteReplacement(rm.group(1) + " " + shiftNumbers(file, rm.group(2))));
                        }
                        rm.appendTail(sb);
                        line = sb.toString();
                    }
                }
                if (!line.equals(orig)) {
                    System.out.println(md.getFileName() + ":" + n + " [" + file + "]\n  - " + orig + "\n  + " + line);
                }
                out.add(line);
            }
            Files.write(md, out, StandardCharsets.UTF_8);
        }
    }

    static String shiftNumbers(String file, String s) {
        TreeMap<Integer, Integer> map = maps.get(file);
        if (map == null || map.isEmpty()) return s;
        Matcher m = Pattern.compile("\\d+").matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int old = Integer.parseInt(m.group());
            Integer exact = map.get(old);
            int now;
            if (exact != null) now = exact;
            else {
                var below = map.floorEntry(old);
                now = below == null ? old : old + below.getValue() - below.getKey();
            }
            m.appendReplacement(sb, String.valueOf(now));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }
}
