package com.wmp.downloader.tools.platform;

import com.wmp.downloader.tools.file.DataControl;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 开机自启动设置工具类
 * <p>
 * 支持平台：
 * <ul>
 *     <li>Windows：注册表 HKCU Run 键（reg 命令）</li>
 *     <li>macOS：LaunchAgent 配置文件（~/Library/LaunchAgents）</li>
 *     <li>Linux：自启动 desktop 文件（~/.config/autostart）</li>
 * </ul>
 */
public class AutoStart {

    private static final Logger logger = Logger.getLogger(AutoStart.class);

    /** Windows 当前用户的开机启动注册表键 */
    private static final String RUN_KEY = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    /** Windows 注册表中使用的值名称 */
    private static final String VALUE_NAME = "SpeedBump";

    /** macOS LaunchAgent 的 Label 及文件名（不含后缀） */
    private static final String MAC_LABEL = "com.wmp.downloader";

    /** Linux 自启动 desktop 文件名 */
    private static final String LINUX_DESKTOP_FILE = "speed-bump.desktop";

    /** Linux 自启动 desktop 文件中显示的应用名称 */
    private static final String LINUX_DISPLAY_NAME = "Speed Bump";

    /** 自启动时附加的启动参数 */
    private static final String BACKGROUND_ARG = "-background";

    private AutoStart() {
    }

    /**
     * 设置或取消开机自启动
     *
     * @param enable true 开启开机自启动；false 取消开机自启动
     * @throws IOException          执行系统命令或文件操作失败
     * @throws InterruptedException 进程被中断
     */
    public static void setAutoStart(boolean enable) throws IOException, InterruptedException {
        if (GetPlatform.isWindows()) {
            if (enable) {
                enableWindows();
            } else {
                disableWindows();
            }
        } else if (GetPlatform.isMac()) {
            if (enable) {
                enableMac();
            } else {
                disableMac();
            }
        } else if (GetPlatform.isLinux()) {
            if (enable) {
                enableLinux();
            } else {
                disableLinux();
            }
        } else {
            throw new UnsupportedOperationException("暂不支持开机自启动的操作系统: " + GetPlatform.getOSName());
        }
    }

    /**
     * 开启开机自启动
     *
     * @throws IOException          执行系统命令或文件操作失败
     * @throws InterruptedException 进程被中断
     */
    public static void enable() throws IOException, InterruptedException {
        setAutoStart(true);
    }

    /**
     * 取消开机自启动
     *
     * @throws IOException          执行系统命令或文件操作失败
     * @throws InterruptedException 进程被中断
     */
    public static void disable() throws IOException, InterruptedException {
        setAutoStart(false);
    }

    /**
     * 获取当前是否已开启开机自启动
     *
     * @return true 已开启；false 未开启或查询失败
     */
    public static boolean isAutoStart() {
        if (GetPlatform.isWindows()) {
            return isAutoStartWindows();
        } else if (GetPlatform.isMac()) {
            return isAutoStartMac();
        } else if (GetPlatform.isLinux()) {
            return isAutoStartLinux();
        }
        return false;
    }

    // ------------------- Windows 实现 -------------------

    private static void enableWindows() throws IOException, InterruptedException {
        File appPath = getAppPathOrThrow();

        // 注册表值形如: "C:\\path\\app.exe" -background
        // 注意: 引号必须用反斜杠转义，否则 reg 会解析失败（路径含空格时退出码 1）或丢失引号
        String command = "\\\"" + appPath.getAbsolutePath() + "\\\" " + BACKGROUND_ARG;
        String[] cmd = {
                "reg", "add", RUN_KEY,
                "/v", VALUE_NAME,
                "/t", "REG_SZ",
                "/d", command,
                "/f"
        };
        execCommand(cmd);
        logger.info("已设置开机自启动 (Windows): " + command);
    }

    private static void disableWindows() throws IOException, InterruptedException {
        String[] cmd = {"reg", "delete", RUN_KEY, "/v", VALUE_NAME, "/f"};
        // 值不存在时 reg 返回 1，同样视为已取消
        execCommand(cmd, 0, 1);
        logger.info("已取消开机自启动 (Windows)");
    }

    private static boolean isAutoStartWindows() {
        try {
            String[] cmd = {"reg", "query", RUN_KEY, "/v", VALUE_NAME};
            Process process = Runtime.getRuntime().exec(cmd);
            return process.waitFor() == 0;
        } catch (Exception e) {
            logger.warn("查询开机自启动状态失败 (Windows)", e);
            return false;
        }
    }

    // ------------------- macOS 实现 -------------------

    private static void enableMac() throws IOException {
        File appPath = getAppPathOrThrow();
        File executable = getMacExecutable(appPath);

        Path launchAgents = Paths.get(System.getProperty("user.home"), "Library", "LaunchAgents");
        Files.createDirectories(launchAgents);
        Path plist = launchAgents.resolve(MAC_LABEL + ".plist");

        String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n" +
                "<plist version=\"1.0\">\n" +
                "<dict>\n" +
                "    <key>Label</key>\n" +
                "    <string>" + escapeXml(MAC_LABEL) + "</string>\n" +
                "    <key>ProgramArguments</key>\n" +
                "    <array>\n" +
                "        <string>" + escapeXml(executable.getAbsolutePath()) + "</string>\n" +
                "        <string>" + BACKGROUND_ARG + "</string>\n" +
                "    </array>\n" +
                "    <key>RunAtLoad</key>\n" +
                "    <true/>\n" +
                "</dict>\n" +
                "</plist>\n";

        // 先卸载旧配置再写入，避免 launchd 持有旧 plist
        runBestEffort("launchctl", "unload", plist.toString());
        Files.writeString(plist, content, StandardCharsets.UTF_8);
        runBestEffort("launchctl", "load", "-w", plist.toString());
        logger.info("已设置开机自启动 (macOS): " + plist);
    }

    private static void disableMac() throws IOException {
        Path plist = Paths.get(System.getProperty("user.home"), "Library", "LaunchAgents", MAC_LABEL + ".plist");
        runBestEffort("launchctl", "unload", plist.toString());
        Files.deleteIfExists(plist);
        logger.info("已取消开机自启动 (macOS)");
    }

    private static boolean isAutoStartMac() {
        return Files.exists(Paths.get(System.getProperty("user.home"), "Library", "LaunchAgents", MAC_LABEL + ".plist"));
    }

    /** 获取 macOS .app 包内真正的可执行文件 */
    private static File getMacExecutable(File appPath) {
        if (appPath.isDirectory() && appPath.getName().endsWith(".app")) {
            String appName = appPath.getName().substring(0, appPath.getName().length() - 4);
            File executable = new File(appPath, "Contents/MacOS/" + appName);
            if (executable.isFile()) {
                return executable;
            }
            throw new IllegalStateException("无法在 .app 中找到可执行文件: " + executable.getAbsolutePath());
        }
        return appPath;
    }

    // ------------------- Linux 实现 -------------------

    private static void enableLinux() throws IOException {
        File appPath = getAppPathOrThrow();

        Path autostartDir = Paths.get(System.getProperty("user.home"), ".config", "autostart");
        Files.createDirectories(autostartDir);
        Path desktopFile = autostartDir.resolve(LINUX_DESKTOP_FILE);

        // Exec 中路径带空格时需要用引号包裹
        String content = "[Desktop Entry]\n" +
                "Type=Application\n" +
                "Name=" + LINUX_DISPLAY_NAME + "\n" +
                "Comment=" + LINUX_DISPLAY_NAME + " 开机自启动\n" +
                "Exec=\"" + appPath.getAbsolutePath() + "\" " + BACKGROUND_ARG + "\n" +
                "Terminal=false\n" +
                "X-GNOME-Autostart-enabled=true\n";
        Files.writeString(desktopFile, content, StandardCharsets.UTF_8);
        logger.info("已设置开机自启动 (Linux): " + desktopFile);
    }

    private static void disableLinux() throws IOException {
        Path desktopFile = Paths.get(System.getProperty("user.home"), ".config", "autostart", LINUX_DESKTOP_FILE);
        Files.deleteIfExists(desktopFile);
        logger.info("已取消开机自启动 (Linux)");
    }

    private static boolean isAutoStartLinux() {
        return Files.exists(Paths.get(System.getProperty("user.home"), ".config", "autostart", LINUX_DESKTOP_FILE));
    }

    // ------------------- 公共辅助 -------------------

    private static File getAppPathOrThrow() {
        File appPath = DataControl.getAppPath();
        if (appPath == null) {
            throw new IllegalStateException("无法获取应用路径（jpackage.app-path 为空），请使用打包后的程序运行");
        }
        return appPath;
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static void execCommand(String[] cmd) throws IOException, InterruptedException {
        execCommand(cmd, 0);
    }

    private static void execCommand(String[] cmd, int... successExitCodes) throws IOException, InterruptedException {
        logger.info("执行: " + String.join(" ", cmd));
        Process process = Runtime.getRuntime().exec(cmd);
        int exit = process.waitFor();
        for (int successExitCode : successExitCodes) {
            if (exit == successExitCode) {
                return;
            }
        }
        throw new IOException("命令执行失败，退出码: " + exit);
    }

    /** 尽力执行命令（如 launchctl），失败仅记录日志，不影响文件配置结果 */
    private static void runBestEffort(String... cmd) {
        try {
            logger.info("执行: " + String.join(" ", cmd));
            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            process.waitFor();
        } catch (Exception e) {
            logger.warn("命令执行失败（忽略）: " + String.join(" ", cmd), e);
        }
    }
}
