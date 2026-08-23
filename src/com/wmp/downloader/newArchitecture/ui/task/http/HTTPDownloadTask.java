package com.wmp.downloader.newArchitecture.ui.task.http;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.FileDownloadTask;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.StatusTipPanel;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class HTTPDownloadTask extends FileDownloadTask {

    private static final Logger logger = Logger.getLogger(HTTPDownloadTask.class);
    private final URI url;
    private final int threadNum;
    private final long fileSize;
    private final int mode;
    private final ArrayList<JProgressBar> threadProgressBarList = new ArrayList<>();
    private final URLDownloadTool.PauseController pauseController = new URLDownloadTool.PauseController();
    private final URLDownloadTool.DownloadProgress downloadProgress = new URLDownloadTool.DownloadProgress();
    private Timer progressTimer;

    private final StatusTipPanel DOWNLOAD_SIZE_PANEL = StatusTipPanel.DOWNLOAD_SIZE_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_SPEED_PANEL = StatusTipPanel.DOWNLOAD_SPEED_CREATOR.create();
    private final StatusTipPanel SHARE_SIZE_PANEL = StatusTipPanel.SHARE_SIZE_CREATOR.create();
    private final StatusTipPanel SHARE_SPEED_PANEL = StatusTipPanel.SHARE_SPEED_CREATOR.create();
    private final StatusTipPanel FILE_MERGE_PANEL = StatusTipPanel.FILE_MERGE_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_FAILED_PANEL = StatusTipPanel.DOWNLOAD_FAILED_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_SUCCESS_PANEL = StatusTipPanel.DOWNLOAD_SUCCESS_CREATOR.create();


    public HTTPDownloadTask(JSONObject jsonObject) {
        super(jsonObject);

        this.fileSize = jsonObject.getLongValue("size", 0);

        this.url = URI.create(jsonObject.getString("url"));
        this.threadNum = jsonObject.getIntValue("threadNum", 0);
        this.mode = jsonObject.getIntValue("threadMode", 0);

        this.addStatusTips(DOWNLOAD_SIZE_PANEL, DOWNLOAD_SPEED_PANEL, FILE_MERGE_PANEL);

    }

    public void doWhenStart() throws Exception {

        this.removeAllStatusTip();
        this.addStatusTips(DOWNLOAD_SIZE_PANEL, DOWNLOAD_SPEED_PANEL, FILE_MERGE_PANEL);


        pauseController.resume();
        downloadProgress.resetSpeed();

        //清除已有的进度条
        ProgressBarsPanel.removeAll();
        threadProgressBarList.clear();

        //判断是否支持多线程


        if (mode == 0 && URLDownloadTool.isCanUseMultithreading(url, fileSize)) {
            for (var i = 0; i < threadNum; i++) {
                var progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(false);
                threadProgressBarList.add(progressBar);

            }
            ProgressBarsPanel.add(
                    UITools.createProgressBarsPanel(
                            UITools.createProgressBarPanel(threadProgressBarList.toArray(JProgressBar[]::new))));
            var downloadingInfo = URLDownloadTool.download(url, savePath, fileName, fileSize, threadNum, 10, threadProgressBarList, pauseController, downloadProgress);

            var latch = downloadingInfo.latch();
            var executor = downloadingInfo.executor();
            var tasks = downloadingInfo.downloadTasks();

            Thread.ofVirtual().start(() -> {
                try {
                    progressTimer = new Timer(1000, e -> {
                        if (isStart) {
                            downloadProgress.updateSpeed();
                            DOWNLOAD_SIZE_PANEL.setText(
                                    URLDownloadTool.DownloadProgress.formatSize(downloadProgress.getDownloadedBytes())
                            );
                            DOWNLOAD_SPEED_PANEL.setText(
                                    URLDownloadTool.DownloadProgress.formatSize(downloadProgress.getSpeed()) + "/s"
                            );
                            FILE_MERGE_PANEL.setText(
                                    URLDownloadTool.DownloadProgress.formatSize(downloadProgress.getMergedBytes())
                            );
                        }
                    });
                    progressTimer.start();
                    // 等待所有任务完成（或异常中断）
                    latch.await();
                    progressTimer.stop();
                    executor.shutdown();
                    // 检查是否有任务失败（通过任务内部标记）
                    boolean hasError = false;
                    for (URLDownloadTool.DownloadTaskRunnable task : tasks) {
                        if (task.hasError()) {
                            hasError = true;
                            break;
                        }
                    }

                    if (hasError) {
                        logger.error("部分分段下载失败，请检查日志后重试。");
                        downloadControlButton.setEnabled(false);
                        removeAllStatusTip();
                        addStatusTip(DOWNLOAD_FAILED_PANEL);
                        ToastMessage.show(this, StringFormat.translate("task", "task.download_task.multi_thread_error"), ToastMessage.ERROR);
                        stop();

                    }
                    logger.debug("下载完成！");

                    //合并文件

                    if (!hasError) {
                        try {
                            //清除已有的进度条
                            ProgressBarsPanel.removeAll();
                            threadProgressBarList.clear();
                            exitButton.setEnabled(false);
                            downloadControlButton.setEnabled(false);

                            downloadProgress.resetMergedBytes();
                            JProgressBar margePartProgressBar = new JProgressBar(0, 100);
                            margePartProgressBar.setStringPainted(false);
                            ProgressBarsPanel.add(UITools.createProgressBarPanel(margePartProgressBar));

                            URLDownloadTool.mergeParts(savePath, fileName, threadNum, fileSize, margePartProgressBar, pauseController, downloadProgress);

                            progressTimer.stop();
                            SwingUtilities.invokeLater(() -> FILE_MERGE_PANEL.setText(StringFormat.formatSize(fileSize)));
                            //清除已有的进度条
                            ProgressBarsPanel.removeAll();
                            threadProgressBarList.clear();

                            downloadControlButton.setEnabled(false);
                            exitButton.setEnabled(true);

                            isFinally = true;

                            removeAllStatusTip();
                            addStatusTip(DOWNLOAD_SUCCESS_PANEL);

                            this.revalidate();
                            this.repaint();
                        } catch (IOException e) {
                            hasError = true;
                            progressTimer.stop();
                            logger.error("合并文件发生异常", e);
                            exitButton.setEnabled(true);
                            downloadControlButton.setEnabled(true);
                            removeAllStatusTip();
                            addStatusTip(DOWNLOAD_FAILED_PANEL);
                            ToastMessage.show(this, StringFormat.translate("task", "task.download_task.merge_error"), ToastMessage.ERROR);
                        }
                    }
                    if (hasError) {
                        JProgressBar progressBar = new JProgressBar(0, 100);
                        progressBar.setStringPainted(false);
                        ProgressBarsPanel.add(UITools.createProgressBarPanel(progressBar));
                        URLDownloadTool.deletePartFiles(fileName, progressBar);
                    }
                } catch (Exception e) {
                    if (progressTimer != null) progressTimer.stop();
                    logger.error("多线程下载发生异常", e);
                    ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_failed_multi"), ToastMessage.ERROR);
                    isStart = false;
                    startCount--;
                    exitButton.setEnabled(true);
                    downloadControlButton.setEnabled(true);
                }
            });


        } else {
            Thread.ofVirtual().start(() -> {
                try {
                    progressTimer = new Timer(1000, e -> {
                        if (isStart) {
                            downloadProgress.updateSpeed();
                            DOWNLOAD_SIZE_PANEL.setText(
                                    URLDownloadTool.DownloadProgress.formatSize(downloadProgress.getDownloadedBytes())
                            );
                            DOWNLOAD_SPEED_PANEL.setText(
                                    URLDownloadTool.DownloadProgress.formatSize(downloadProgress.getSpeed()) + "/s"
                            );
                            FILE_MERGE_PANEL.setText(
                                    URLDownloadTool.DownloadProgress.formatSize(downloadProgress.getMergedBytes())
                            );
                        }
                    });
                    progressTimer.start();
                    JProgressBar progressBar = new JProgressBar(0, 100);
                    progressBar.setStringPainted(false);
                    threadProgressBarList.add(progressBar);
                    ProgressBarsPanel.add(UITools.createProgressBarPanel(progressBar));
                    var isSuccess = URLDownloadTool.singleThreadDownload(url, savePath, fileName, fileSize, 10, progressBar, pauseController, downloadProgress);
                    progressTimer.stop();
                    if (!isSuccess) {
                        downloadControlButton.setEnabled(false);
                        removeAllStatusTip();
                        addStatusTip(DOWNLOAD_FAILED_PANEL);
                        ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_failed_single"), ToastMessage.ERROR);
                        stop();
                    } else {
                        isFinally = true;
                        removeAllStatusTip();
                        addStatusTip(DOWNLOAD_SUCCESS_PANEL);
                    }
                    downloadControlButton.setEnabled(false);
                    //清除已有的进度条
                    ProgressBarsPanel.removeAll();
                    threadProgressBarList.clear();

                    this.revalidate();
                    this.repaint();
                } catch (Exception e) {
                    if (progressTimer != null) progressTimer.stop();
                    logger.error("单线程下载发生异常", e);
                    ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_failed_single"), ToastMessage.ERROR);
                    isStart = false;
                    startCount--;
                }
            });
        }


    }

    @Override
    public void doWhenRestart() throws Exception {
        pauseController.resume();
        if (progressTimer != null) progressTimer.restart();

    }

    public void doWhenStop() {
        pauseController.pause();
        progressTimer.stop();

        if (progressTimer != null) progressTimer.stop();

    }

    @Override
    public void doWhenExit() {
        if (progressTimer != null) progressTimer.stop();
        threadProgressBarList.clear();
    }
}
