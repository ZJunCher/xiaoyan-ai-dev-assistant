package com.xiaoyan.aiassistant.document;

// 文档结构特征，用于判断分块策略。
public record DocumentStructureFeatures(
        int headingCount,
        int paragraphCount,
        double headingRatio,
        double headingDensity,
        double averageParagraphLength,
        double formatMarkerDensity,
        int headingDepth,
        double continuousTextRatio,
        int structureScore
) {
}

