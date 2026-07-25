package com.wmp.downloader.test.video;

import org.bytedeco.javacv.FFmpegFrameGrabber;

import java.util.Scanner;

public class VideoInfoReader {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("请输入视频文件路径：");
        String filePath = sc.nextLine();
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(filePath);
        grabber.start();

        // 1. 获取视频容器格式
        String format = grabber.getFormat();
        System.out.println("容器格式: " + format); // 例如: mp4, mov, avi

        // 2. 获取视频流信息
        int videoStreamIndex = grabber.getVideoStream();
        if (videoStreamIndex >= 0) {
            // 视频编码格式, 例如: h264, hevc, mpeg4[reference:2]
            String videoCodec = grabber.getVideoCodecName();
            // 视频分辨率
            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            // 视频帧率
            double frameRate = grabber.getFrameRate();
            // 视频比特率 (bps)
            long videoBitrate = grabber.getVideoBitrate();

            System.out.println("视频编码: " + videoCodec);
            System.out.println("分辨率: " + width + "x" + height);
            System.out.println("帧率: " + frameRate);
            System.out.println("视频比特率: " + videoBitrate);
        }

        // 3. 获取音频流信息
        int audioStreamIndex = grabber.getAudioStream();
        if (audioStreamIndex >= 0) {
            // 音频编码格式, 例如: aac, mp3
            String audioCodec = grabber.getAudioCodecName();
            // 音频采样率 (Hz)
            int sampleRate = grabber.getSampleRate();
            // 音频声道数
            int audioChannels = grabber.getAudioChannels();

            System.out.println("音频编码: " + audioCodec);
            System.out.println("采样率: " + sampleRate);
            System.out.println("声道数: " + audioChannels);
        }

        // 4. 其他元数据 (如时长)
        double duration = grabber.getLengthInTime() / 1_000_000.0; // 微秒转秒
        System.out.println("视频时长: " + duration + " 秒");

        grabber.stop();
    }
}
