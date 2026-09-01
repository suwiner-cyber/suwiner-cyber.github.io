package com.xiaoxiaoshuo.reader;

import java.util.ArrayList;
import java.util.List;

public final class AiTypesetter {
    private AiTypesetter() {}

    public static String formatNovel(String raw) {
        if (raw == null) return "";
        String s = raw.replace('\u3000', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
        String[] lines = s.split("\\n");
        List<String> out = new ArrayList<>();
        StringBuilder paragraph = new StringBuilder();
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                flush(out, paragraph);
                continue;
            }
            boolean title = t.matches("^(第.{1,12}[章节卷回部篇].*)$") || t.length() <= 20 && t.matches(".*[卷章回篇]$");
            if (title) {
                flush(out, paragraph);
                out.add(t.replace(" ", ""));
                continue;
            }
            if (paragraph.length() > 0 && shouldBreak(paragraph.charAt(paragraph.length()-1), t.charAt(0))) {
                flush(out, paragraph);
            }
            paragraph.append(t.replaceAll("(?<=[\\u4e00-\\u9fa5]) +(?=[\\u4e00-\\u9fa5])", ""));
            if (endsSentence(t)) flush(out, paragraph);
        }
        flush(out, paragraph);
        return String.join("\n\n", out).replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static String compactIntro(String raw) {
        return formatNovel(raw).replace("\n", "").replaceAll("\\s+", "").trim();
    }

    private static boolean endsSentence(String s) {
        return s.endsWith("。") || s.endsWith("！") || s.endsWith("？") || s.endsWith("……") || s.endsWith("；") || s.endsWith("”");
    }
    private static boolean shouldBreak(char prev, char next) {
        return (prev=='。'||prev=='！'||prev=='？'||prev=='；'||prev=='”') && next!='，' && next!='。';
    }
    private static void flush(List<String> out, StringBuilder p) {
        if (p.length() > 0) {
            String t = p.toString().trim();
            if (!t.isEmpty()) out.add(t);
            p.setLength(0);
        }
    }
}
