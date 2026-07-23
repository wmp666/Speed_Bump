package com.wmp.downloader.ui.task.http;

import com.wmp.downloader.laug.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.download.URLDownloadTool.DownloadProgress;
import com.wmp.downloader.tools.download.URLDownloadTool.PauseController;
import com.wmp.downloader.ui.task.DownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

public class URLDownloadTask extends DownloadTask {

    private static final Logger logger = Logger.getLogger(URLDownloadTask.class);
    private final URI url;
    private final int threadNum;
    private final long fileSize;
    private final int mode;
    private final ArrayList<JProgressBar> threadProgressBarList = new ArrayList<>();
    private URLDownloadTool.PauseController pauseController = new PauseController();
    private URLDownloadTool.DownloadProgress downloadProgress = new DownloadProgress();
    private Timer progressTimer;

    /**
     * 创建一个下载任务
     *
     * @param fileName  文件名
     * @param url       文件地址
     * @param savePath  保存路径（不包括文件）
     * @param threadNum 线程数
     */
    public URLDownloadTask(String fileName, long fileSize, URI url, File savePath, int threadNum, int mode) {
        super(fileName, savePath);


        this.fileSize = fileSize;

        this.url = url;
        this.threadNum = threadNum;
        this.mode = mode;

    }


    public void doWhenStart() throws Exception {

        pauseController.resume();
        downloadProgress.resetSpeed();

        pauseController = new PauseController();
        downloadProgress = new DownloadProgress();

        //清除已有的进度条
        ProgressBarsPanel.removeAll();
        threadProgressBarList.clear();

        //判断是否支持多线程


        if (mode == 0 && URLDownloadTool.isCanUseMultithreading(url, fileSize)) {
            for (var i = 0; i < threadNum; i++) {
                var progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                threadProgressBarList.add(progressBar);
                ProgressBarsPanel.add(progressBar);
            }
            var downloadingInfo = URLDownloadTool.download(url, savePath, fileName, fileSize, threadNum, 10, threadProgressBarList, pauseController, downloadProgress);

            var latch = downloadingInfo.latch();
            var executor = downloadingInfo.executor();
            var tasks = downloadingInfo.downloadTasks();

            Thread.ofVirtual().start(() -> {
                try {
                    progressTimer = new Timer(1000, e -> {
                        if (isStart) {
                            downloadProgress.updateSpeed();
                            infoLabel.setText(String.format(StringFormat.translate("task", "task.download_task.progress_multi"),
                                    DownloadProgress.formatSize(downloadProgress.getDownloadedBytes()),
                                    DownloadProgress.formatSize(downloadProgress.getSpeed()),
                                    DownloadProgress.formatSize(downloadProgress.getMergedBytes())));
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
                        infoLabel.setText(StringFormat.translate("task", "task.download_task.multi_thread_error"));
                        JOptionPane.showMessageDialog(this, StringFormat.translate("task", "task.download_task.download_failed_multi"), StringFormat.translate("common", "error"), JOptionPane.ERROR_MESSAGE);
                        stop();

                    }
                    logger.debug("下载完成！");

                    //合并文件

                    if (!hasError) {
                        try {
                            //清除已有的进度条
                            ProgressBarsPanel.removeAll();
                            threadProgressBarList.clear();

                            downloadProgress.resetMergedBytes();
                            SwingUtilities.invokeLater(() -> infoLabel.setText(StringFormat.translate("task", "task.download_task.merging_file")));
                            JProgressBar margePartProgressBar = new JProgressBar(0, 100);
                            margePartProgressBar.setStringPainted(true);
                            ProgressBarsPanel.add(margePartProgressBar);

                            URLDownloadTool.mergeParts(savePath, fileName, threadNum, fileSize, margePartProgressBar, pauseController, downloadProgress);

                            progressTimer.stop();
                            SwingUtilities.invokeLater(() -> infoLabel.setText(""));
                            //清除已有的进度条
                            ProgressBarsPanel.removeAll();
                            threadProgressBarList.clear();

                            downloadControlButton.setEnabled(false);

                            isFinally = true;

                            infoLabel.setText(String.format(StringFormat.translate("task", "task.download_task.download_complete"), DownloadProgress.formatSize(fileSize)));

                            this.revalidate();
                            this.repaint();
                        } catch (IOException e) {
                            hasError = true;
                            progressTimer.stop();
                            logger.error("合并文件发生异常", e);
                            downloadControlButton.setEnabled(false);
                            infoLabel.setText(StringFormat.translate("task", "task.download_task.merge_error"));
                            JOptionPane.showMessageDialog(this, StringFormat.translate("task", "task.download_task.download_failed_merge"), StringFormat.translate("common", "error"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    if (hasError) {
                        JProgressBar progressBar = new JProgressBar(0, 100);
                        progressBar.setStringPainted(true);
                        ProgressBarsPanel.add(progressBar);
                        URLDownloadTool.deletePartFiles(fileName, progressBar);
                    }
                } catch (Exception e) {
                    if (progressTimer != null) progressTimer.stop();
                    logger.error("多线程下载发生异常", e);
                    JOptionPane.showMessageDialog(this, StringFormat.translate("task", "task.download_task.download_failed_multi"), StringFormat.translate("common", "error"), JOptionPane.ERROR_MESSAGE);
                    isStart = false;
                }
            });


        } else {
            Thread.ofVirtual().start(() -> {
                try {
                    progressTimer = new Timer(1000, e -> {
                        if (isStart) {
                            downloadProgress.updateSpeed();
                            infoLabel.setText(String.format(StringFormat.translate("task", "task.download_task.download_complete"),
                                    DownloadProgress.formatSize(downloadProgress.getDownloadedBytes()),
                                    DownloadProgress.formatSize(downloadProgress.getSpeed()),
                                    DownloadProgress.formatSize(downloadProgress.getMergedBytes())));
                        }
                    });
                    progressTimer.start();
                    JProgressBar progressBar = new JProgressBar(0, 100);
                    progressBar.setStringPainted(true);
                    threadProgressBarList.add(progressBar);
                    ProgressBarsPanel.add(progressBar);
                    var isSuccess = URLDownloadTool.singleThreadDownload(url, savePath, fileName, fileSize, 10, progressBar, pauseController, downloadProgress);
                    progressTimer.stop();
                    if (!isSuccess) {
                        downloadControlButton.setEnabled(false);
                        infoLabel.setText(StringFormat.translate("task", "task.download_task.download_failed_single"));
                        JOptionPane.showMessageDialog(this, StringFormat.translate("task", "task.download_task.download_failed_single"), StringFormat.translate("common", "error"), JOptionPane.ERROR_MESSAGE);
                        stop();
                    } else {
                        isFinally = true;
                        infoLabel.setText(String.format(StringFormat.translate("task", "task.download_task.download_complete"), DownloadProgress.formatSize(fileSize)));
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
                    JOptionPane.showMessageDialog(this, StringFormat.translate("task", "task.download_task.download_failed_single"), StringFormat.translate("common", "error"), JOptionPane.ERROR_MESSAGE);
                    isStart = false;
                }
            });
        }


    }

    public void doWhenStop() {
        pauseController.pause();
        progressTimer.stop();
        infoLabel.setText(StringFormat.translate("task", "task.download_task.paused"));

        if (progressTimer != null) progressTimer.stop();

    }

    @Override
    public void doWhenExit() {
        if (progressTimer != null) progressTimer.stop();
        threadProgressBarList.clear();
    }

}
