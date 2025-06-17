package com.entangle.analysis.util;

public class FileSizeFormatUtil {
    /**
     * バイト数を指定単位または自動でKB/MB/GB等に変換し、桁数も指定できる共通メソッド。
     * @param bytes バイト数
     * @param unit  "auto"で自動、"KB"/"MB"/"GB"/"TB"等も指定可
     * @param decimalPlaces 小数点以下の桁数
     * @return 例: "1.23 MB"
     */
    public static String formatBytes(long bytes, String unit, int decimalPlaces) {
        double value = bytes;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int idx = 0;
        if (unit == null || unit.isEmpty() || unit.equalsIgnoreCase("auto")) {
            while (value >= 1024 && idx < units.length - 1) {
                value /= 1024;
                idx++;
            }
        } else {
            for (int i = 0; i < units.length; i++) {
                if (units[i].equalsIgnoreCase(unit)) {
                    value = value / Math.pow(1024, i);
                    idx = i;
                    break;
                }
            }
        }
        return String.format("%." + decimalPlaces + "f %s", value, units[idx]);
    }

    public static String formatBytes(long bytes, String unit) {
        return formatBytes(bytes, unit, 0);
    }

    public static String formatBytes(long bytes) {
        return formatBytes(bytes, "auto", 0);
    }
}
