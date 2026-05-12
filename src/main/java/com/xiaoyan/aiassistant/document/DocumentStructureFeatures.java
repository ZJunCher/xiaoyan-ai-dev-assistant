package com.xiaoyan.aiassistant.document;

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

