package com.wmp.downloader.ui.task.ed2k;

import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.ui.task.DownloadTask;

import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

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
