package com.wmp.downloader.tools.download;

import org.apache.log4j.Logger;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;

import javax.swing.*;
import java.io.File;
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

        if (!destPath.exists()){
            try {
                destPath.getParentFile().mkdirs();
                destPath.createNewFile();
            } catch (Exception e) {
                logger.error("Error creating destination file: ", e);
                return false;
            }
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

    static void main() {
        var progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        JFrame frame = new JFrame();
        frame.add(progressBar);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);


        //扫描器
        var scanner = new Scanner(System.in);

        System.out.print("请输入视频文件路径：");
        var videoPath = new File(scanner.next());
        System.out.print("\n请输入音频文件路径：");
        var audioPath = new File(scanner.next());
        System.out.print("\n请输入目标输出文件路径：");
        var destPath = new File(scanner.next());
        System.out.println();
        IO.readln("" + ConvergenceTool.converge(videoPath,
                audioPath,
                destPath, progressBar));
    }
}