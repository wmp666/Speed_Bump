package com.wmp.downloader.ui.task.douyin;

import com.wmp.downloader.ui.task.http.URLDownloadTask;

import java.io.File;
import java.net.URI;

public class DouyinVideoDownloadTask extends URLDownloadTask {
    /**
     * 创建一个下载任务
     *
     * @param fileName  文件名
     * @param fileSize  文件大小
     * @param url       文件地址
     * @param savePath  保存路径（不包括文件）
     * @param threadNum 线程数
     * @param mode      下载模式
     */
    public DouyinVideoDownloadTask(String fileName, long fileSize, URI url, File savePath, int threadNum, int mode) {
        super(fileName, fileSize, url, savePath, threadNum, 1);
    }
}
