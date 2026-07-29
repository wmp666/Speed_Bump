package com.wmp.downloader.test;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ED2KMonitor {

    // 正则表达式，用于匹配 show dl 命令的输出行
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile(
            "\\[([\\d.]+)%\\]\\s+(\\d+)/\\s*(\\d+).*?-\\s*([\\d.]+)\\s*(\\w+)/s"
    );

    /**
     * 执行amulecmd命令并返回其输出
     */
    private static String executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder( "amulecmd", "-c", command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("命令执行失败，退出码: " + exitCode);
        }
        return output.toString();
    }

    /**
     * 解析 show dl 命令的输出，提取第一个任务的进度信息
     */
    public static void getDownloadProgress() {
        try {
            String output = executeCommand("show dl");
            // 按行分割，查找匹配的行
            for (String line : output.split("\n")) {
                Matcher matcher = DOWNLOAD_PATTERN.matcher(line);
                if (matcher.find()) {
                    String progress = matcher.group(1);     // 例如: "4.9"
                    String downloaded = matcher.group(2);   // 例如: "12"
                    String total = matcher.group(3);        // 例如: "13"
                    String speed = matcher.group(4);        // 例如: "5.78"
                    String unit = matcher.group(5);         // 例如: "kB"

                    System.out.println("下载进度: " + progress + "%");
                    System.out.println("已下载: " + downloaded + " MB / " + total + " MB");
                    System.out.println("下载速度: " + speed + " " + unit + "/s");
                    return; // 只处理第一个匹配的任务
                }
            }
            System.out.println("当前没有正在进行的下载任务。");

        } catch (IOException | InterruptedException e) {
            System.err.println("获取下载进度失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 示例：定期调用此方法以监控下载进度
        getDownloadProgress();
    }
}
