package com.wmp.downloader.ui.task.ed2k;

import com.wmp.downloader.ui.task.DownloadTask;

import java.io.File;

public class Ed2kDownloadTask extends DownloadTask {


    public Ed2kDownloadTask(String fileName, File savePath) {
        super(fileName, savePath);
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
