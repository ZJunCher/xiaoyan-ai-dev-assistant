package com.xiaoyan.aiassistant.document;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentStructureAnalyzer {
    static final Pattern HEADING = Pattern.compile(
            "^(#{1,6}\\s+.+|第[一二三四五六七八九十百千万0-9]+[章节部分].+|[0-9]+(\\.[0-9]+)*[、.\\s].+)$");
    private static final Pattern FORMAT_MARKER = Pattern.compile(
            "(^\\s*[-*+]\\s+)|(^\\s*\\d+[.)、]\\s+)|(```)|(^\\s*\\|.+\\|\\s*$)");

    // 识别文档结构特征，供路由器决定用结构分块、语义分块还是混合分块。
    public DocumentStructureFeatures analyze(String text) {
        String safeText = text == null ? "" : text;
        String[] paragraphs = safeText.split("\\n\\s*\\n");
        int paragraphCount = 0;
        int headingCount = 0;
        int totalParagraphLength = 0;
        int continuousTextLength = 0;
        int maxHeadingDepth = 0;
        int markerCount = 0;

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }

            // 段落越长且标题越少，越像 FAQ、经验总结这类弱结构文本。
            paragraphCount++;
            totalParagraphLength += trimmed.length();
            if (trimmed.length() >= 500) {
                continuousTextLength += trimmed.length();
            }

            // 只用首行判断标题，避免把长段落里的普通句子误识别成标题。
            String firstLine = trimmed.lines().findFirst().orElse(trimmed).trim();
            if (isHeading(firstLine, trimmed.length())) {
                headingCount++;
                maxHeadingDepth = Math.max(maxHeadingDepth, headingDepth(firstLine));
            }
            markerCount += countFormatMarkers(trimmed);
        }

        double headingRatio = paragraphCount == 0 ? 0 : headingCount * 1.0 / paragraphCount;
        double headingDensity = safeText.isBlank() ? 0 : headingCount * 1000.0 / safeText.length();
        double averageParagraphLength = paragraphCount == 0 ? 0 : totalParagraphLength * 1.0 / paragraphCount;
        double formatMarkerDensity = safeText.isBlank() ? 0 : markerCount * 1000.0 / safeText.length();
        double continuousTextRatio = safeText.isBlank() ? 0 : continuousTextLength * 1.0 / Math.max(1, safeText.length());
        int score = score(headingCount, headingRatio, headingDensity, formatMarkerDensity, maxHeadingDepth, continuousTextRatio);

        // 分数只做轻量路由，不作为绝对分类，避免复杂文档被过早定死。
        return new DocumentStructureFeatures(headingCount, paragraphCount, headingRatio, headingDensity,
                averageParagraphLength, formatMarkerDensity, maxHeadingDepth, continuousTextRatio, score);
    }

    // 判断标题时限制块长度，防止普通长段落刚好命中标题格式。
    public boolean isHeading(String line, int blockLength) {
        return StringUtils.hasText(line) && blockLength < 180 && HEADING.matcher(line.trim()).matches();
    }

    // 用简单可解释的规则打分，方便后续根据线上样本调权重。
    private int score(int headingCount, double headingRatio, double headingDensity,
                      double formatMarkerDensity, int headingDepth, double continuousTextRatio) {
        int score = 0;
        if (headingCount >= 2) {
            score++;
        }
        if (headingRatio >= 0.12) {
            score++;
        }
        if (headingDensity >= 4) {
            score++;
        }
        if (formatMarkerDensity >= 3) {
            score++;
        }
        if (headingDepth >= 2) {
            score++;
        }
        if (continuousTextRatio >= 0.55) {
            score--;
        }
        return score;
    }

    // Markdown 标题按 # 数量算层级，数字标题按 1.2.3 的段数算层级。
    private int headingDepth(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) {
            int depth = 0;
            while (depth < trimmed.length() && trimmed.charAt(depth) == '#') {
                depth++;
            }
            return depth;
        }
        Matcher matcher = Pattern.compile("^[0-9]+(\\.[0-9]+)*").matcher(trimmed);
        if (matcher.find()) {
            return matcher.group().split("\\.").length;
        }
        return 1;
    }

    // 统计列表、代码块、表格等格式标记，用于判断文档结构是否明显。
    private int countFormatMarkers(String text) {
        int count = 0;
        for (String line : text.split("\\n")) {
            if (FORMAT_MARKER.matcher(line).find()) {
                count++;
            }
        }
        return count;
    }
}
