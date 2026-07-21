package com.wmp.downloader.tools;

public class BiliInfoFormat {
    public static final int V_Q_144 = 5;
    public static final int V_Q_240 = 6;
    public static final int V_Q_360 = 16;
    public static final int V_Q_480 = 32;
    public static final int V_Q_720 = 48;
    public static final int V_Q_720_LOGIN = 64;
    public static final int V_Q_720_HFR = 74;
    public static final int V_Q_1080 = 80;
    public static final int V_Q_AI_ENHANCE = 100;
    public static final int V_Q_1080_PLUS = 112;
    public static final int V_Q_1080P60 = 116;
    public static final int V_Q_4K = 120;
    public static final int V_Q_HDR = 125;
    public static final int V_Q_DOLBY = 126;
    public static final int V_Q_8K = 127;

    public static final int S_Q_64 = 30216;
    public static final int S_Q_132 = 30232;
    public static final int S_Q_192 = 30280;
    public static final int S_Q_DOLBY = 30250;
    public static final int S_Q_HIRES = 30251;

    public static final int V_CODE_H264 = 7;
    public static final int V_CODE_H265 = 12;
    public static final int V_CODE_AV1 = 13;

    public static String VideoFormat(int VQuality){
        return switch (VQuality){
            case V_Q_144 -> "144P 极速";
            case V_Q_240 -> "240P 极速";
            case V_Q_360 -> "360P 流畅";
            case V_Q_480 -> "480P 清晰";
            case V_Q_720, V_Q_720_LOGIN -> "720P 高清";
            case V_Q_720_HFR -> "720P 高帧率";
            case V_Q_1080 -> "1080P 高清";
            case V_Q_AI_ENHANCE -> "智能修复";
            case V_Q_1080_PLUS -> "1080P+ 高码率";
            case V_Q_1080P60 -> "1080P60 高帧率";
            case V_Q_4K -> "4K 超清";
            case V_Q_HDR -> "HDR 真彩";
            case V_Q_DOLBY -> "杜比视界";
            case V_Q_8K -> "8K 超高清";
            default -> "Unknown";
        };
    }

    public static String AudioFormat(int AQuality){
        return switch (AQuality){
            case S_Q_64 -> "64kbps";
            case S_Q_132 -> "132kbps";
            case S_Q_192 -> "192kbps";
            case S_Q_DOLBY -> "杜比全景声";
            case S_Q_HIRES -> "Hi-Res 无损";
            default -> "Unknown";
        };
    }

    public static String[] getVideoFormats(int[] VQualities){
        String[] formats = new String[VQualities.length];
        for (int i = 0; i < VQualities.length; i++) {
            formats[i] = VideoFormat(VQualities[i]);
        }
        return formats;
    }

    public static String[] getAudioFormats(int[] AQualities){
        String[] formats = new String[AQualities.length];
        for (int i = 0; i < AQualities.length; i++) {
            formats[i] = AudioFormat(AQualities[i]);
        }
        return formats;
    }

    public static String getVideoCode(int VCode){
        return switch (VCode){
            case V_CODE_H264 -> "H.264 / AVC";
            case V_CODE_H265 -> "H.265 / HEVC";
            case V_CODE_AV1 -> "AV1";
            default -> "Unknown";
        };
    }
}
