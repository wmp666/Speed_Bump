package com.wmp.downloader.tools;

import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

public class StringFormat {
    private static final String BUNDLE_PREFIX = "com.wmp.downloader.laug.";

    public static String translate(String rootLocal, String key) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX + rootLocal, Locale.getDefault());
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return String.format("%s: %s", rootLocal, key);
        }
    }

    /**
     * 根据当前操作系统净化文件名或目录名，将非法字符替换为 '_'
     * <p>
     * Windows 禁用字符：\ / : * ? " < > |
     * Unix/Linux 禁用字符：/ 和 NUL (空字符)
     *
     * @param name 原始名称（不含路径）
     * @return 净化后的名称，如果输入为 null 或空则原样返回
     */
    public static String sanitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        String os = System.getProperty("os.name").toLowerCase();
        String regex;
        if (os.contains("win")) {
            regex = "[\\\\/:*?\"<>|]";
        } else {
            // Unix/Linux/macOS：只禁止 '/' 和 NUL 字符
            regex = "[/\0]";
        }
        return name.replaceAll(regex, "_");
    }

    /**
     * 净化整个文件路径（除根目录外），递归处理每一级目录和最终文件名
     *
     * @param file 原始 File 对象（可表示文件或目录）
     * @return 新的 File 对象，其路径中所有非根目录的名称都已净化，根目录保持不变
     */
    public static File sanitizeFile(File file) {
        if (file == null) {
            return null;
        }

        File parent = file.getParentFile();
        String name = file.getName();

        // 如果是根目录（无父目录且名称为空），直接返回原对象
        if (parent == null && name.isEmpty()) {
            return file;
        }

        // 净化当前名称（目录名或文件名）
        String sanitizedName = sanitizeName(name);

        if (parent == null) {
            // 没有父目录，可能是一个相对路径的单独文件/目录
            // 如果名称没变，返回原对象，否则新建
            if (sanitizedName.equals(name)) {
                return file;
            } else {
                return new File(sanitizedName);
            }
        } else {
            // 递归净化父目录
            File sanitizedParent = sanitizeFile(parent);
            // 如果父目录和当前名称都没变，返回原对象
            if (sanitizedParent.equals(parent) && sanitizedName.equals(name)) {
                return file;
            }
            return new File(sanitizedParent, sanitizedName);
        }
    }

        /**
         * 将纳秒数格式化为最合适的单位字符串（自动选择 ns、μs、ms、s、min、h、d、y）
         *
         * @param nanos 纳秒数（可为负数，将显示负号）
         * @return 格式化后的字符串，例如 "1.23 ms"、"5.00 s"、"150 ns"
         */
        public static String formatNanos(long nanos) {
            if (nanos == 0) return "0 ns";

            // 处理负数
            if (nanos < 0) {
                return "-" + formatNanos(-nanos);
            }

            // 单位定义（从大到小）
            long[] thresholds = {
                    31_536_000_000_000_000L, // 1 year  (365天)
                    86_400_000_000_000L,     // 1 day
                    3_600_000_000_000L,      // 1 hour
                    60_000_000_000L,         // 1 minute
                    1_000_000_000L,          // 1 second
                    1_000_000L,              // 1 millisecond
                    1_000L                   // 1 microsecond
            };

            String[] units = {"y", "d", "h", "min", "s", "ms", "μs"};

            // 从最大的单位开始尝试
            for (int i = 0; i < thresholds.length; i++) {
                if (nanos >= thresholds[i]) {
                    double value = (double) nanos / thresholds[i];
                    // 数值较大时只保留整数，避免冗长的小数
                    if (value >= 100) {
                        return String.format("%.0f %s", value, units[i]);
                    } else {
                        return String.format("%.2f %s", value, units[i]);
                    }
                }
            }

            // 小于 1 微秒（≤ 999 ns）
            return nanos + " ns";
        }
}