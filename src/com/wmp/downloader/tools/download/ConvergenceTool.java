package com.wmp.downloader.tools.download;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConvergenceTool {

    private static final Logger logger = Logger.getLogger(ConvergenceTool.class);
    // ---------- 硬件加速自动检测 ----------
    private static HardwareAccelConfig cachedConfig = null;

    /**
     * 将分离的视频流和音频流合并为一个完整的MP4文件
     *
     * @param videoPath   视频文件（.m4s）
     * @param audioPath   音频文件（.m4s）
     * @param destPath    合并后的目标输出文件路径
     * @param progressBar 用于展示合并进度的进度条组件
     * @return 合并成功返回true，失败返回false
     */
    public static boolean converge(File videoPath, File audioPath, File destPath, JProgressBar progressBar) {

        destPath = StringFormat.sanitizeFile(destPath);
        if (!destPath.exists()) {
            try {
                destPath.getParentFile().mkdirs();
                destPath.createNewFile();
            } catch (Exception e) {
                logger.error("Error creating destination file: ", e);
                return false;
            }
        }
        logger.info("保存路径：" + destPath.getAbsolutePath());

        return LocalConvergeWithStreamCopy(DataControl.get("ffmpeg_appPath", ""), StringFormat.sanitizeFile(videoPath), StringFormat.sanitizeFile(audioPath), StringFormat.sanitizeFile(destPath), progressBar);

    }

    /**
     * 本地流拷贝模式（最快，不重新编码）
     * 通过调用本地 FFmpeg 实现 -c copy，速度远超帧级重编码
     *
     * @param appPath FFmpeg 可执行文件所在的 bin 目录路径
     */
    private static boolean LocalConvergeWithStreamCopy(String appPath, File videoPath, File audioPath,
                                                       File destPath, JProgressBar progressBar) {
        try {
            if (progressBar != null) {
                progressBar.setIndeterminate(true);
                progressBar.setString("正在合并...");
            }

            String ffmpegExe = findFFmpeg(appPath);

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegExe, "-y",
                    "-i", videoPath.getAbsolutePath(),
                    "-i", audioPath.getAbsolutePath(),
                    "-c", "copy",
                    "-map", "0:v:0",
                    "-map", "1:a:0",
                    "-movflags", "+faststart",
                    destPath.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("FFmpeg 流拷贝失败，退出码: " + exitCode + "\n" + output);
                return false;
            }

            if (progressBar != null) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                });
            }

            logger.info("流拷贝模式合并完成");
            return true;

        } catch (Exception e) {
            logger.error("流拷贝模式合并失败: ", e);
            return false;
        }
    }

    /**
     * 在 appPath 目录中查找 FFmpeg 可执行文件，找不到则回退到系统 PATH
     */
    private static String findFFmpeg(String appPath) {
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String exeName = isWindows ? "ffmpeg.exe" : "ffmpeg";

        if (appPath != null) {
            File localExe = new File(appPath, exeName);
            if (localExe.isFile()) {
                logger.info("使用本地 FFmpeg: " + localExe.getAbsolutePath());
                return localExe.getAbsolutePath();
            }
        }

        logger.info("本地未找到 FFmpeg，使用系统 PATH");
        return exeName;
    }

    /**
     * 将视频转码/重新封装为指定的容器格式与编码
     *
     * @param inputFile       源视频文件
     * @param outputFile      目标输出文件（路径及文件名）
     * @param containerFormat 目标容器格式扩展名，如 "mp4", "mkv"
     * @param videoCodec      视频编码器名称（FFmpeg 编码器名），如 "libx264"
     * @param audioCodec      音频编码器名称（FFmpeg 编码器名），如 "aac"
     * @param progressBar     进度条（可为 null）
     * @return 成功返回 true
     */
    public static boolean transcodeVideo(File inputFile, File outputFile,
                                         String containerFormat, String videoCodec, String audioCodec,
                                         JProgressBar progressBar) {
        outputFile = StringFormat.sanitizeFile(outputFile);
        if (!outputFile.exists()) {
            try {
                outputFile.getParentFile().mkdirs();
                outputFile.createNewFile();
            } catch (Exception e) {
                logger.error("创建输出文件失败: ", e);
                return false;
            }
        } else {
            if (JOptionPane.showConfirmDialog(null,
                    StringFormat.translate("task", "task.download_task.delete_exists_file.confirm")) == JOptionPane.YES_OPTION) {
                DataControl.deleteFolder(outputFile, true);
            } else return false;

        }

        // 优先使用本地 FFmpeg（速度快，支持更多编码）
        return localTranscode(DataControl.get("ffmpeg_appPath", ""),
                inputFile, outputFile, containerFormat, videoCodec, audioCodec, progressBar);

    }

    private static boolean localTranscode(String appPath, File inputFile, File outputFile,
                                          String containerFormat, String videoCodec, String audioCodec,
                                          JProgressBar progressBar) {
        try {
            if (progressBar != null) {
                progressBar.setIndeterminate(true);
                progressBar.setString("正在转码...");
            }

            String ffmpeg = findFFmpeg(appPath);
            File ffmpegFile = new File(ffmpeg);
            File ffmpegDir = ffmpegFile.getParentFile();

            // ---------- 硬件加速处理 ----------
            String usedVideoCodec = videoCodec;
            String hwaccelParam = null;
            boolean useHw = DataControl.get("is_use_hardware_acceleration", true);

            if (useHw) {
                HardwareAccelConfig config = detectBestHardwareConfig(appPath);
                if (config != null && !"none".equals(config.hwaccel)) {
                    hwaccelParam = config.hwaccel;
                    // 如果用户原编码器是软件编码，则替换为硬件编码器
                    if ("libx264".equals(videoCodec) || "libx265".equals(videoCodec)) {
                        usedVideoCodec = config.videoEncoder;
                    } else {
                        // 用户可能指定了其他编码器，保留但添加 hwaccel 仍能加速解码
                        usedVideoCodec = videoCodec;
                    }
                    logger.info("启用硬件加速: hwaccel=" + hwaccelParam + ", encoder=" + usedVideoCodec);
                } else {
                    logger.info("未检测到可用硬件加速，使用软件编码");
                }
            }

            // ---------- 构建命令 ----------
            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(ffmpeg);
            cmd.add("-y");
            if (hwaccelParam != null) {
                cmd.add("-hwaccel");
                cmd.add(hwaccelParam);
                // 可选：添加 -hwaccel_output_format 以保持 GPU 内存，但可能引起兼容问题，这里不添加
            }
            cmd.add("-i");
            cmd.add(inputFile.getAbsolutePath());
            cmd.add("-c:v");
            cmd.add(usedVideoCodec);
            cmd.add("-c:a");
            cmd.add(audioCodec);
            cmd.add("-f");
            cmd.add(containerFormat);
            cmd.add(outputFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(cmd);

            // 设置工作目录和环境变量（同原有逻辑）
            if (ffmpegDir != null && ffmpegDir.exists()) {
                pb.directory(ffmpegDir);
            }
            Map<String, String> env = pb.environment();
            String pathEnv = env.get("PATH");
            if (ffmpegDir != null) {
                String ffmpegPath = ffmpegDir.getAbsolutePath();
                if (pathEnv == null) {
                    env.put("PATH", ffmpegPath);
                } else if (!pathEnv.contains(ffmpegPath)) {
                    env.put("PATH", ffmpegPath + File.pathSeparator + pathEnv);
                }
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 消费输出流（使用虚拟线程）
            Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.debug("FFmpeg output: " + line);
                        // 这里可以解析进度（如 out_time_ms）并更新 progressBar
                    }
                } catch (Exception e) {
                    logger.error("日志输出异常：", e);
                }
            });

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("FFmpeg 转码失败，退出码: " + exitCode);
                ToastMessage.show(null, "FFmpeg 转码失败，退出码: " + exitCode, ToastMessage.ERROR);
                return false;
            }

            if (progressBar != null) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                });
            }
            return true;
        } catch (Exception e) {
            logger.error("本地 FFmpeg 转码异常: ", e);
            ToastMessage.show(null, "本地 FFmpeg 转码异常: " + e, ToastMessage.ERROR);
            return false;
        }
    }

    /**
     * 使用本地 ffmpeg 检测最优硬件加速配置（仅当 ffmpeg 可用时）
     */
    private static HardwareAccelConfig detectBestHardwareConfig(String ffmpegPath) {
        if (cachedConfig != null) return cachedConfig;

        String ffmpeg = findFFmpeg(ffmpegPath); // 获取可执行文件路径
        HardwareAccelConfig fallback = new HardwareAccelConfig("none", "libx264", "yuv420p");

        try {
            // 1. 获取支持的 hwaccel
            Set<String> hwaccels = new HashSet<>();
            Process p1 = new ProcessBuilder(ffmpeg, "-hwaccels").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p1.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.trim().startsWith(" ")) {
                        hwaccels.add(line.trim());
                    }
                }
            }
            p1.waitFor();

            // 2. 获取支持的编码器
            Set<String> encoders = new HashSet<>();
            Process p2 = new ProcessBuilder(ffmpeg, "-encoders").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p2.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    // 匹配形如 "V..... libx264" 的行
                    if (line.matches("^[VAS]....\\s+\\S+")) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) encoders.add(parts[1]);
                    }
                }
            }
            p2.waitFor();

            // 3. 按优先级选择
            String[] hwPriority = {"cuda", "nvdec", "qsv", "vaapi", "vdpau", "videotoolbox"};
            String[] encPriority = {"h264_nvenc", "h264_qsv", "h264_vaapi", "h264_amf", "h264_videotoolbox"};

            String selectedHw = null;
            String selectedEnc = null;
            for (String hw : hwPriority) {
                if (hwaccels.contains(hw)) {
                    // 根据 hw 推断编码器前缀
                    String prefix = getEncoderPrefix(hw);
                    for (String enc : encPriority) {
                        if (enc.startsWith(prefix) && encoders.contains(enc)) {
                            selectedHw = hw;
                            selectedEnc = enc;
                            break;
                        }
                    }
                    if (selectedHw != null) break;
                }
            }

            if (selectedHw != null) {
                String pixelFormat = (selectedHw.equals("qsv") || selectedHw.equals("vaapi")) ? "nv12" : "yuv420p";
                cachedConfig = new HardwareAccelConfig(selectedHw, selectedEnc, pixelFormat);
                logger.info("检测到硬件加速: hwaccel=" + selectedHw + ", encoder=" + selectedEnc);
                return cachedConfig;
            }
        } catch (Exception e) {
            logger.warn("硬件加速检测失败，将使用软件编码", e);
        }

        cachedConfig = fallback;
        return cachedConfig;
    }

    private static String getEncoderPrefix(String hwaccel) {
        switch (hwaccel) {
            case "cuda":
            case "nvdec":
                return "nvenc";
            case "qsv":
                return "qsv";
            case "vaapi":
                return "vaapi";
            case "vdpau":
                return "vdpau";
            case "videotoolbox":
                return "videotoolbox";
            default:
                return "";
        }
    }

    private static String normalizeCodecName(String name) {
        if (name == null) return "libx264";
        switch (name.toLowerCase()) {
            case "libx265":
            case "hevc":
            case "h265":
            case "hevc_nvenc":   // 如果用户直接传入硬件编码器名
            case "hevc_qsv":
            case "hevc_vaapi":
                return "libx265";
            case "libx264":
            case "h264":
            case "h.264":
                return "libx264";
            default:
                return name; // 原样保留
        }
    }

    private static String normalizeAudioCodecName(String name) {
        if (name == null) return "aac";
        switch (name.toLowerCase()) {
            case "aac":
                return "aac";
            case "mp3":
            case "libmp3lame":
                return "libmp3lame";
            case "flac":
                return "flac";
            default:
                return name;
        }
    }

    /**
     * 硬件加速配置
     */
    private static class HardwareAccelConfig {
        final String hwaccel;          // 如 "cuda", "qsv"，或 "none"
        final String videoEncoder;     // 如 "h264_nvenc"
        final String pixelFormat;      // 如 "nv12"

        HardwareAccelConfig(String hwaccel, String videoEncoder, String pixelFormat) {
            this.hwaccel = hwaccel;
            this.videoEncoder = videoEncoder;
            this.pixelFormat = pixelFormat;
        }
    }
}