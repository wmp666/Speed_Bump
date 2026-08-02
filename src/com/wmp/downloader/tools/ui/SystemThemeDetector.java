package com.wmp.downloader.tools.ui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class SystemThemeDetector {

    /**
     * 检测当前系统是否为深色模式
     * @return true=深色，false=浅色（或无法检测时默认浅色）
     */
    public static boolean isDarkMode() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                return detectWindows();
            } else if (os.contains("mac")) {
                return detectMac();
            } else if (os.contains("linux") || os.contains("nix")) {
                return detectLinux();
            }
        } catch (Exception e) {
            // 任何异常都默认返回浅色
        }
        return false; // 默认浅色
    }

    private static boolean detectWindows() throws Exception {
        // 读取注册表: AppsUseLightTheme = 1 为浅色，0 为深色
        Process process = Runtime.getRuntime().exec(
                new String[]{"reg", "query", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "/v", "AppsUseLightTheme"}
        );
        process.waitFor(3, TimeUnit.SECONDS);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("AppsUseLightTheme")) {
                    // 输出格式类似: "    AppsUseLightTheme    REG_DWORD    0x1"
                    String[] parts = line.trim().split("\\s+");
                    String value = parts[parts.length - 1];
                    return "0x0".equals(value); // 0x0 表示深色
                }
            }
        }
        return false;
    }

    private static boolean detectMac() throws Exception {
        // defaults read -g AppleInterfaceStyle 存在且返回 Dark 表示深色
        Process process = Runtime.getRuntime().exec(
                new String[]{"defaults", "read", "-g", "AppleInterfaceStyle"}
        );
        process.waitFor(3, TimeUnit.SECONDS);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            return line != null && line.trim().contains("Dark");
        }
    }

    private static boolean detectLinux() throws Exception {
        // 尝试 GNOME 的 gsettings
        Process process = Runtime.getRuntime().exec(
                new String[]{"gsettings", "get", "org.gnome.desktop.interface", "gtk-theme"}
        );
        process.waitFor(3, TimeUnit.SECONDS);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && line.toLowerCase().contains("dark")) {
                return true;
            }
        }

        // 尝试 KDE Plasma 的 kdeglobals
        Process process2 = Runtime.getRuntime().exec(
                new String[]{"bash", "-c", "grep -i 'ColorScheme=' ~/.config/kdeglobals | head -1"}
        );
        process2.waitFor(3, TimeUnit.SECONDS);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process2.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && line.toLowerCase().contains("dark")) {
                return true;
            }
        }

        return false;
    }
}