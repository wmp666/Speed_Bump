package com.wmp.downloader.tools.platform;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * 跨平台文件关联注册工具
 */
public class FileAssociation {

    /**
     * 注册文件关联（自动检测操作系统）
     *
     * @param extension     文件扩展名（不带点，例如 "myapp"）
     * @param description   文件类型描述（例如 "我的应用程序文件"）
     * @param iconPath      图标文件路径（Windows: .ico, macOS: .icns, Linux: .png）
     * @param appPath       应用程序路径（Windows: 可执行文件或启动脚本；macOS: .app 目录；Linux: 可执行文件）
     * @throws IOException          执行系统命令失败
     * @throws InterruptedException 进程被中断
     */
    public static void register(String extension, String description, String iconPath, String appPath)
            throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            registerWindows(extension, description, iconPath, appPath);
        } else if (os.contains("mac")) {
            registerMac(extension, description, iconPath, appPath);
        } else if (os.contains("nix") || os.contains("nux")) {
            registerLinux(extension, description, iconPath, appPath);
        } else {
            throw new UnsupportedOperationException("不支持的操作系统: " + os);
        }
    }

    // ------------------- Windows 实现 -------------------
    private static void registerWindows(String ext, String desc, String icon, String app) throws IOException, InterruptedException {
        String fileType = ext + "file";
        String quotedApp = "\"" + app + "\"";
        String command = quotedApp + " \"%1\"";

        // 关键修改：将所有注册表路径从 HKCR 改为 HKCU\Software\Classes
        List<String> commands = Arrays.asList(
                "reg add HKCU\\Software\\Classes\\." + ext + " /ve /t REG_SZ /d \"" + fileType + "\" /f",
                "reg add HKCU\\Software\\Classes\\" + fileType + " /ve /t REG_SZ /d \"" + desc + "\" /f",
                "reg add HKCU\\Software\\Classes\\" + fileType + "\\DefaultIcon /ve /t REG_SZ /d \"" + icon + "\" /f",
                "reg add HKCU\\Software\\Classes\\" + fileType + "\\shell\\open\\command /ve /t REG_SZ /d \"" + command + "\" /f"
        );

        for (String cmd : commands) {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            pb.inheritIO();
            Process p = pb.start();
            int exit = p.waitFor();
            if (exit != 0) {
                throw new IOException("执行命令失败: " + cmd + "，退出码: " + exit);
            }
        }
        System.out.println("当前用户的文件关联注册成功。");
    }
    // ------------------- macOS 实现 -------------------
    private static void registerMac(String ext, String desc, String icon, String app) throws IOException, InterruptedException {
        // macOS 需要应用是 .app bundle，且 Info.plist 已包含文档类型声明
        // 我们通过 lsregister 刷新 Launch Services 数据库，使配置生效
        // 注意：此方法仅当 .app 已存在且 Info.plist 配置正确时才有效
        File appDir = new File(app);
        if (!appDir.exists() || !appDir.isDirectory() || !appDir.getName().endsWith(".app")) {
            throw new IllegalArgumentException("macOS 需要提供一个有效的 .app 目录路径");
        }

        // 使用 lsregister 注册 .app
        String lsregister = "/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister";
        ProcessBuilder pb = new ProcessBuilder(lsregister, "-f", app);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("lsregister 执行失败，退出码: " + exit);
        }
        System.out.println("macOS 文件关联已刷新（需确保 .app 的 Info.plist 已声明 " + ext + " 类型）。");
    }

    // ------------------- Linux 实现 -------------------
    private static void registerLinux(String ext, String desc, String icon, String app) throws IOException, InterruptedException {
        // 用户目录下的配置目录
        String home = System.getProperty("user.home");
        Path localApps = Paths.get(home, ".local", "share", "applications");
        Path localMime = Paths.get(home, ".local", "share", "mime", "packages");
        Path mimeApps = Paths.get(home, ".local", "share", "applications", "mimeapps.list");

        Files.createDirectories(localApps);
        Files.createDirectories(localMime);

        // 1. 生成 .desktop 文件
        String desktopName = ext + "file.desktop";
        Path desktopFile = localApps.resolve(desktopName);
        String mimeType = "application/x-" + ext;
        String content = "[Desktop Entry]\n" +
                "Name=" + desc + "\n" +
                "Comment=" + desc + "\n" +
                "Exec=" + app + " %f\n" +
                "Icon=" + icon + "\n" +
                "Type=Application\n" +
                "MimeType=" + mimeType + ";\n";
        Files.write(desktopFile, content.getBytes("UTF-8"));

        // 2. 生成 MIME 类型定义
        Path mimeFile = localMime.resolve(ext + ".xml");
        String mimeXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<mime-info xmlns=\"http://www.freedesktop.org/standards/shared-mime-info\">\n" +
                "    <mime-type type=\"" + mimeType + "\">\n" +
                "        <comment>" + desc + "</comment>\n" +
                "        <glob pattern=\"*." + ext + "\"/>\n" +
                "    </mime-type>\n" +
                "</mime-info>";
        Files.write(mimeFile, mimeXml.getBytes("UTF-8"));

        // 3. 更新 MIME 数据库
        ProcessBuilder pbUpdate = new ProcessBuilder("update-mime-database", localMime.getParent().toString());
        pbUpdate.inheritIO();
        Process p1 = pbUpdate.start();
        if (p1.waitFor() != 0) {
            throw new IOException("update-mime-database 失败");
        }

        // 4. 设置为默认应用程序（写入 mimeapps.list）
        // 读取已有配置，若没有则新建
        Map<String, String> defaults = new HashMap<>();
        if (Files.exists(mimeApps)) {
            for (String line : Files.readAllLines(mimeApps)) {
                if (line.startsWith("[Default Applications]")) {
                    // 跳过标题，后续解析
                } else if (line.contains("=") && !line.startsWith("[")) {
                    String[] parts = line.split("=", 2);
                    defaults.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        defaults.put(mimeType, desktopName);

        // 重写文件
        List<String> lines = new ArrayList<>();
        lines.add("[Default Applications]");
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            lines.add(entry.getKey() + "=" + entry.getValue());
        }
        Files.write(mimeApps, lines);

        System.out.println("Linux 文件关联注册成功。");
    }


    // ------------------- 公共删除接口 -------------------
    /**
     * 删除文件关联（自动检测操作系统）
     *
     * @param extension 文件扩展名（不带点，例如 "myapp"）
     * @param appPath   应用程序路径（Windows: 可忽略；macOS: .app 目录；Linux: 可忽略）
     * @throws IOException          执行系统命令失败
     * @throws InterruptedException 进程被中断
     */
    public static void unregister(String extension, String appPath) throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            unregisterWindows(extension);
        } else if (os.contains("mac")) {
            unregisterMac(appPath);
        } else if (os.contains("nix") || os.contains("nux")) {
            unregisterLinux(extension);
        } else {
            throw new UnsupportedOperationException("不支持的操作系统: " + os);
        }
    }

    // ------------------- Windows 删除实现 -------------------
    private static void unregisterWindows(String ext) throws IOException, InterruptedException {
        String fileType = ext + "file";
        List<String> commands = Arrays.asList(
                "reg delete HKCU\\Software\\Classes\\." + ext + " /f",
                "reg delete HKCU\\Software\\Classes\\" + fileType + " /f"
        );
        for (String cmd : commands) {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", cmd);
            pb.inheritIO();
            Process p = pb.start();
            int exit = p.waitFor();
            // 删除不存在的项时 reg 返回 0（成功）或 1（未找到），我们均视为成功（已不存在）
            if (exit != 0 && exit != 1) {
                throw new IOException("执行命令失败: " + cmd + "，退出码: " + exit);
            }
        }
        System.out.println("Windows 文件关联已删除。");
    }

    // ------------------- macOS 删除实现 -------------------
    private static void unregisterMac(String appPath) throws IOException, InterruptedException {
        File appDir = new File(appPath);
        if (!appDir.exists() || !appDir.isDirectory() || !appDir.getName().endsWith(".app")) {
            throw new IllegalArgumentException("macOS 需要提供一个有效的 .app 目录路径");
        }
        String lsregister = "/System/Library/Frameworks/CoreServices.framework/Frameworks/LaunchServices.framework/Support/lsregister";
        // -u 表示取消注册
        ProcessBuilder pb = new ProcessBuilder(lsregister, "-u", appPath);
        pb.inheritIO();
        Process p = pb.start();
        int exit = p.waitFor();
        if (exit != 0) {
            throw new IOException("lsregister -u 执行失败，退出码: " + exit);
        }
        System.out.println("macOS 文件关联已删除（需确保 .app 的 Info.plist 已移除 " + appPath + " 的声明）。");
    }

    // ------------------- Linux 删除实现 -------------------
    private static void unregisterLinux(String ext) throws IOException, InterruptedException {
        String home = System.getProperty("user.home");
        Path localApps = Paths.get(home, ".local", "share", "applications");
        Path localMime = Paths.get(home, ".local", "share", "mime", "packages");
        Path mimeApps = Paths.get(home, ".local", "share", "applications", "mimeapps.list");

        String desktopName = ext + "file.desktop";
        Path desktopFile = localApps.resolve(desktopName);
        String mimeType = "application/x-" + ext;
        Path mimeFile = localMime.resolve(ext + ".xml");

        // 1. 删除 .desktop 文件
        boolean desktopDeleted = Files.deleteIfExists(desktopFile);
        // 2. 删除 MIME 定义文件
        boolean mimeDeleted = Files.deleteIfExists(mimeFile);

        // 3. 从 mimeapps.list 中移除该 MIME 类型的默认关联
        if (Files.exists(mimeApps)) {
            List<String> lines = Files.readAllLines(mimeApps);
            List<String> newLines = new ArrayList<>();
            boolean inDefaultSection = false;
            for (String line : lines) {
                if (line.startsWith("[Default Applications]")) {
                    inDefaultSection = true;
                    newLines.add(line);
                    continue;
                }
                if (inDefaultSection && line.startsWith("[")) {
                    inDefaultSection = false;
                }
                if (inDefaultSection && line.startsWith(mimeType + "=")) {
                    continue; // 跳过该条目
                }
                newLines.add(line);
            }
            // 重新写入（如果内容变化）
            if (!newLines.equals(lines)) {
                Files.write(mimeApps, newLines);
            }
        }

        // 4. 更新 MIME 数据库
        ProcessBuilder pbUpdate = new ProcessBuilder("update-mime-database", localMime.getParent().toString());
        pbUpdate.inheritIO();
        Process p1 = pbUpdate.start();
        if (p1.waitFor() != 0) {
            throw new IOException("update-mime-database 失败");
        }

        System.out.println("Linux 文件关联删除成功。");
    }

}
