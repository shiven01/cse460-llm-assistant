package com.cse460.llm_assistant.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class DiagramDetector {

    /**
     * Simple heuristic to determine if an image is likely to be a diagram/chart
     * This is a basic implementation and can be enhanced with ML-based approaches
     *
     * @param image The image to analyze
     * @return true if the image is likely a diagram/chart
     */
    public static boolean isDiagram(BufferedImage image) {
        // Some heuristics to determine if an image is a diagram/chart:

        // 1. Check for color diversity - charts often have limited colors
        int colorCount = countDistinctColors(image, 100); // Sample 100 points

        // 2. Check for line patterns - diagrams often have straight lines
        boolean hasLinePatterns = detectLinePatterns(image);

        // Simple decision rule: if few colors and line patterns detected, probably a diagram
        return colorCount < 20 && hasLinePatterns;
    }

    private static int countDistinctColors(BufferedImage image, int sampleSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Sample points from the image
        Set<Integer> colors = new HashSet<>();
        for (int i = 0; i < sampleSize; i++) {
            int x = (int) (Math.random() * width);
            int y = (int) (Math.random() * height);
            int rgb = image.getRGB(x, y);
            colors.add(rgb);
        }

        return colors.size();
    }

    private static boolean detectLinePatterns(BufferedImage image) {
        // This is a simplified approach - more sophisticated approaches would use
        // edge detection algorithms like Hough Transform

        int width = image.getWidth();
        int height = image.getHeight();

        // Sample horizontal and vertical lines
        int horizontalLineCount = 0;
        int verticalLineCount = 0;

        // Check for horizontal lines
        for (int y = height / 4; y < 3 * height / 4; y += height / 10) {
            int prevColor = image.getRGB(0, y);
            int sameColorRun = 1;
            for (int x = 1; x < width; x++) {
                int color = image.getRGB(x, y);
                if (color == prevColor) {
                    sameColorRun++;
                    if (sameColorRun > width / 3) {
                        horizontalLineCount++;
                        break;
                    }
                } else {
                    sameColorRun = 1;
                    prevColor = color;
                }
            }
        }

        // Check for vertical lines
        for (int x = width / 4; x < 3 * width / 4; x += width / 10) {
            int prevColor = image.getRGB(x, 0);
            int sameColorRun = 1;
            for (int y = 1; y < height; y++) {
                int color = image.getRGB(x, y);
                if (color == prevColor) {
                    sameColorRun++;
                    if (sameColorRun > height / 3) {
                        verticalLineCount++;
                        break;
                    }
                } else {
                    sameColorRun = 1;
                    prevColor = color;
                }
            }
        }

        return horizontalLineCount >= 2 || verticalLineCount >= 2;
    }
}