package com.wmp.downloader.ui.task.ed2k;

import com.wmp.downloader.ui.task.DownloadTask;

import java.io.File;

public class Ed2kDownloadTask extends DownloadTask {

    private final String ed2kLink;
    private final long fileSize;


    public Ed2kDownloadTask(String fileName, File savePath, long fileSize, String ed2kLink) {
        super(fileName, savePath);
        this.fileSize = fileSize;
        this.ed2kLink = ed2kLink;
    }

    @Override
    public void doWhenExit() {

    }

    @Override
    public void doWhenStart() throws Exception {

    }

    @Override
    public void doWhenStop() {

    }
}
