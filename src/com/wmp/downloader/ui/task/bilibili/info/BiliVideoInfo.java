package com.wmp.downloader.ui.task.bilibili.info;

/**
 * B站视频信息
 * @param codecid 编码
 * @param url 视频地址
 */
public record BiliVideoInfo(int codecid, int quality, String url, long size) {
}
