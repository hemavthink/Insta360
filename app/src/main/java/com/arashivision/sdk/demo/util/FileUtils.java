package com.arashivision.sdk.demo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FileUtils {

    private final static long KB = 1024;
    private final static long MB = KB * 1024;
    private final static long GB = MB * 1024;

    /**
     * 将文件大小转换为KB、MB、GB 等字符串
     *
     * @param b 文件字节大小
     * @return B（整数位）、KB（整数位）、MB（四舍五入，两位精确度）、GB（四舍五入，两位精确度）
     */
    public static String computeFileSize(long b) {
        float size;
        BigDecimal decimal;

        if (b / GB >= 1) {
            size = b / (float) GB;
            decimal = new BigDecimal(size);
            return decimal.setScale(2, RoundingMode.HALF_UP).floatValue() + "G";
        } else if (b / MB >= 1) {
            size = b / (float) MB;
            decimal = new BigDecimal(size);
            return decimal.setScale(2, RoundingMode.HALF_UP).floatValue() + "M";
        } else if (b / KB >= 1) {
            size = b / (float) KB;
            decimal = new BigDecimal(size);
            return decimal.setScale(0, RoundingMode.HALF_UP).intValue() + "K";
        } else {
            return b + "B";
        }
    }

    /**
     * 将文件大小由KB、MB、GB 等字符串转换为long值
     *
     * @param fileSize 文件大小字符串
     * @return 文件大小
     */
    public static long parseFileSize(String fileSize) {
        try {
            double size = Double.parseDouble(fileSize.replaceAll("[^-*\\d+(\\.)?]", "")); // 提取数字
            if (fileSize.toUpperCase().contains("G")) {
                size *= 1024 * 1024 * 1024;
            } else if (fileSize.toUpperCase().contains("M")) {
                size *= 1024 * 1024;
            } else if (fileSize.toUpperCase().contains("K")) {
                size *= 1024;
            }
            return (long) size;
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }

}
