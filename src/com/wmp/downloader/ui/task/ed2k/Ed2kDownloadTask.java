package com.wmp.downloader.ui.task.ed2k;

import com.wmp.downloader.ui.task.gopeed.GopeedDownloadTask;

import java.io.File;

public class Ed2kDownloadTask extends GopeedDownloadTask {
    public Ed2kDownloadTask(String fileName, File savePath, long size, String ed2kLink) {
        super(fileName, savePath, size, ed2kLink);
    }
}
