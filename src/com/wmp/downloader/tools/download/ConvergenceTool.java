package com.wmp.downloader.tools.download;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import org.apache.log4j.Logger;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Scanner;

public class ConvergenceTool {

    private static final Logger logger = Logger.getLogger(ConvergenceTool.class);

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
        if (!destPath.exists()){
            try {
                destPath.getParentFile().mkdirs();
                destPath.createNewFile();
            } catch (Exception e) {
                logger.error("Error creating destination file: ", e);
                return false;
            }
        }
        logger.info("保存路径：" + destPath.getAbsolutePath());

        if (DataControl.get("ffmpeg_isUseLocal", false)){
            return LocalConvergeWithStreamCopy(DataControl.get("ffmpeg_appPath", ""), StringFormat.sanitizeFile(videoPath), StringFormat.sanitizeFile(audioPath), StringFormat.sanitizeFile(destPath), progressBar);
        }

        // ---------- 初始化进度条（建议调用前设置好 min/max） ----------
        if (progressBar != null) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
        }

        FFmpegFrameRecorder recorder = null;

        try(FFmpegFrameGrabber videoGrabber = new FFmpegFrameGrabber(videoPath);
            FFmpegFrameGrabber audioGrabber = new FFmpegFrameGrabber(audioPath);) {
            FFmpegLogCallback.set();

            videoGrabber.start();
            audioGrabber.start();

            // ---------- 获取总帧数用于进度计算 ----------
            int totalFrames = videoGrabber.getLengthInFrames();
            long totalDuration = videoGrabber.getLengthInTime() / 1000; // 微秒转毫秒（备用方案）


            recorder = new FFmpegFrameRecorder(destPath, videoGrabber.getImageWidth(), videoGrabber.getImageHeight());
            recorder.setVideoCodec(videoGrabber.getVideoCodec());
            recorder.setAudioCodec(audioGrabber.getAudioCodec());
            recorder.setFormat("mp4");
            recorder.setFrameRate(videoGrabber.getFrameRate());
            recorder.setVideoBitrate(videoGrabber.getVideoBitrate());
            recorder.setVideoOption("preset", "ultrafast");
            recorder.setVideoOption("tune", "zerolatency");

            recorder.setSampleRate(audioGrabber.getSampleRate());
            recorder.setAudioChannels(audioGrabber.getAudioChannels());
            recorder.start();

            Frame videoFrame;
            int frameCount = 0;
            int lastProgress = -1;

            while ((videoFrame = videoGrabber.grabImage()) != null) {
                Frame audioFrame = audioGrabber.grabSamples();
                if (audioFrame != null) {
                    recorder.record(audioFrame);
                }
                recorder.record(videoFrame);

                frameCount++;
                if (totalFrames > 0) {
                    int progress = Math.clamp((int) ((double) frameCount / totalFrames * 100), 0, 100);
                    if (progress != lastProgress && progressBar != null) {
                        lastProgress = progress;
                        final int p = progress;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(p);
                            progressBar.setString(p + "%");
                        });
                    }
                }
            }

            Frame audioFrame;
            while ((audioFrame = audioGrabber.grabSamples()) != null) {
                recorder.record(audioFrame);
            }

            if (progressBar != null) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                });
            }

            recorder.close();

            return true;

        } catch (Exception e) {
            logger.error("Error occurred during convergence: ", e);
            return false;
        } finally {
            if (recorder != null) {
                try {
                    recorder.stop();
                    recorder.release();
                } catch (FFmpegFrameRecorder.Exception e) {
                    logger.error("Error stopping or releasing recorder: ", e);
                }
            }
        }
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
            FFmpegLogCallback.set();

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
        }else{
            if (JOptionPane.showConfirmDialog(null,
                    StringFormat.translate("task", "task.download_task.delete_exists_file.confirm")) == JOptionPane.YES_OPTION) {
                DataControl.deleteFolder(outputFile, true);
            }else return false;

        }

        // 优先使用本地 FFmpeg（速度快，支持更多编码）
        if (DataControl.get("ffmpeg_isUseLocal", false)) {
            return localTranscode(DataControl.get("ffmpeg_appPath", ""),
                    inputFile, outputFile, containerFormat, videoCodec, audioCodec, progressBar);
        }

        // 否则使用 JavaCV 内置方式（不依赖外部 FFmpeg）
        return javacvTranscode(inputFile, outputFile, containerFormat, videoCodec, audioCodec, progressBar);
    }

    private static boolean localTranscode(String appPath, File inputFile, File outputFile,
                                          String containerFormat, String videoCodec, String audioCodec,
                                          JProgressBar progressBar) {
        try {
            FFmpegLogCallback.set();
            if (progressBar != null) {
                progressBar.setIndeterminate(true);
                progressBar.setString("正在转码...");
            }

            String ffmpeg = findFFmpeg(appPath);
            File ffmpegFile = new File(ffmpeg);
            File ffmpegDir = ffmpegFile.getParentFile();

            ProcessBuilder pb = new ProcessBuilder(
                    ffmpeg, "-y",
                    "-i", inputFile.getAbsolutePath(),
                    "-c:v", videoCodec,
                    "-c:a", audioCodec,
                    "-f", containerFormat,
                    outputFile.getAbsolutePath()
            );

            // 1. 设置工作目录为 ffmpeg 所在目录（防止相对路径依赖）
            if (ffmpegDir != null && ffmpegDir.exists()) {
                pb.directory(ffmpegDir);
            }

            // 2. 将 ffmpeg 目录添加到 PATH 环境变量（确保依赖库可找到）
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

            // 3. 【关键】必须消费输出流，否则进程阻塞
            Thread.ofVirtual().start(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 可在此解析进度（例如提取时间），目前仅调试日志
                        logger.debug("FFmpeg output: " + line);
                    }
                } catch (Exception e) {
                    logger.error("日志输出异常：", e);
                }
            });

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("FFmpeg 转码失败，退出码: " + exitCode);
                JOptionPane.showMessageDialog(null, "FFmpeg 转码失败，退出码: " + exitCode);

                // 可进一步读取错误信息（但已合并到同一流）
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
            JOptionPane.showMessageDialog(null, "本地 FFmpeg 转码异常: " + e);
            return false;
        }
    }

    private static boolean javacvTranscode(File inputFile, File outputFile,
                                           String containerFormat, String videoCodec, String audioCodec,
                                           JProgressBar progressBar) {
        if (progressBar != null) {
            progressBar.setMinimum(0);
            progressBar.setMaximum(100);
            progressBar.setValue(0);
            progressBar.setStringPainted(true);
        }

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
            FFmpegLogCallback.set();
            grabber.start();

            int totalFrames = grabber.getLengthInFrames();
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile,
                    grabber.getImageWidth(), grabber.getImageHeight());
            recorder.setFormat(containerFormat);
            recorder.setVideoCodec(findVideoCodecID(videoCodec));
            recorder.setAudioCodec(findAudioCodecID(audioCodec));
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setVideoBitrate(grabber.getVideoBitrate());
            recorder.setSampleRate(grabber.getSampleRate());
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.start();

            Frame frame;
            int count = 0;
            int lastProgress = -1;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
                count++;
                if (totalFrames > 0 && progressBar != null) {
                    int progress = Math.clamp((int) ((double) count / totalFrames * 100), 0, 99);
                    if (progress != lastProgress) {
                        lastProgress = progress;
                        final int p = progress;
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(p);
                            progressBar.setString(p + "%");
                        });
                    }
                }
            }
            recorder.close();
            if (progressBar != null) {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                });
            }
            return true;
        } catch (Exception e) {
            logger.error("JavaCV 转码失败: ", e);
            return false;
        }
    }

    // 将编码器名称转为 FFmpeg 内部 ID（用于 JavaCV）
    private static int findVideoCodecID(String codecName) {
        switch (codecName) {
            case "libx264": return avcodec.AV_CODEC_ID_H264;
            case "libx265": return avcodec.AV_CODEC_ID_HEVC;
            default: return avcodec.AV_CODEC_ID_H264;
        }
    }

    private static int findAudioCodecID(String codecName) {
        switch (codecName) {
            case "aac": return avcodec.AV_CODEC_ID_AAC;
            case "libmp3lame": return avcodec.AV_CODEC_ID_MP3;
            case "flac": return avcodec.AV_CODEC_ID_FLAC;
            case "pcm_s16le": return avcodec.AV_CODEC_ID_PCM_S16LE;
            default: return avcodec.AV_CODEC_ID_AAC;
        }
    }
}