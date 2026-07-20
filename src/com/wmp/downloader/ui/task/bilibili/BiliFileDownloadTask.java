package com.wmp.downloader.ui.task.bilibili;

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
import java.util.LinkedHashMap;
import java.util.Map;

public class BiliFileDownloadTask extends DownloadTask {

    private static final Logger logger = Logger.getLogger(BiliFileDownloadTask.class);

    private final URI url;
    private final int threadNum;
    private final long fileSize;
    private final int mode;
    private final ArrayList<JProgressBar> threadProgressBarList = new ArrayList<>();
    private PauseController pauseController = new PauseController();
    private DownloadProgress downloadProgress = new DownloadProgress();
    private Timer progressTimer;

    public BiliFileDownloadTask(String fileName, long fileSize, URI url, File savePath, int threadNum, int mode) {
        super(fileName, savePath);
        this.fileSize = fileSize;
        this.url = url;
        this.threadNum = threadNum;
        this.mode = mode;
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", "https://www.bilibili.com");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        return headers;
    }

    public void doWhenStart() throws Exception {
        pauseController.resume();
        downloadProgress.resetSpeed();

        pauseController = new PauseController();
        downloadProgress = new DownloadProgress();

        ProgressBarsPanel.removeAll();
        threadProgressBarList.clear();

        Map<String, String> headers = buildHeaders();

        if (mode == 0 && URLDownloadTool.isCanUseMultithreading(url, fileSize, headers)) {
            for (var i = 0; i < threadNum; i++) {
                var progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                threadProgressBarList.add(progressBar);
                ProgressBarsPanel.add(progressBar);
            }
            var downloadingInfo = URLDownloadTool.download(url, savePath, fileName, fileSize, threadNum, 10, threadProgressBarList, pauseController, downloadProgress, headers);

            var latch = downloadingInfo.latch();
            var executor = downloadingInfo.executor();
            var tasks = downloadingInfo.downloadTasks();

            Thread.ofVirtual().start(() -> {
                try {
                    progressTimer = new Timer(1000, e -> {
                        if (isStart) {
                            downloadProgress.updateSpeed();
                            infoLabel.setText(String.format("<html>已下载: %s | 速度: %s/s | 已合并: %s</html>",
                                    DownloadProgress.formatSize(downloadProgress.getDownloadedBytes()),
                                    DownloadProgress.formatSize(downloadProgress.getSpeed()),
                                    DownloadProgress.formatSize(downloadProgress.getMergedBytes())));
                        }
                    });
                    progressTimer.start();
                    latch.await();
                    progressTimer.stop();
                    executor.shutdown();

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
                        infoLabel.setText("多线程下载异常");
                        JOptionPane.showMessageDialog(this, "下载失败\n多线程下载异常", "错误", JOptionPane.ERROR_MESSAGE);
                        stop();
                    }
                    logger.debug("下载完成！");

                    if (!hasError) {
                        try {
                            ProgressBarsPanel.removeAll();
                            threadProgressBarList.clear();

                            downloadProgress.resetMergedBytes();
                            SwingUtilities.invokeLater(() -> infoLabel.setText("正在合并文件"));
                            JProgressBar mergeProgressBar = new JProgressBar(0, 100);
                            mergeProgressBar.setStringPainted(true);
                            ProgressBarsPanel.add(mergeProgressBar);

                            URLDownloadTool.mergeParts(savePath, fileName, threadNum, fileSize, mergeProgressBar, pauseController, downloadProgress);

                            progressTimer.stop();
                            SwingUtilities.invokeLater(() -> infoLabel.setText(""));
                            ProgressBarsPanel.removeAll();
                            threadProgressBarList.clear();

                            downloadControlButton.setEnabled(false);
                            isFinally = true;
                            infoLabel.setText(String.format("<html>下载完成 | 大小: %s</html>", DownloadProgress.formatSize(fileSize)));

                            this.revalidate();
                            this.repaint();
                        } catch (IOException e) {
                            hasError = true;
                            progressTimer.stop();
                            logger.error("合并文件发生异常", e);
                            downloadControlButton.setEnabled(false);
                            infoLabel.setText("合并文件发生异常");
                            JOptionPane.showMessageDialog(this, "下载失败\n合并文件发生异常", "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    if (hasError) {
                        JProgressBar progressBar = new JProgressBar(0, 100);
                        progressBar.setStringPainted(true);
                        ProgressBarsPanel.add(progressBar);
                        URLDownloadTool.deletePartFiles(fileName, progressBar);
                    }
                } catch (Exception e) {
                    logger.error("多线程下载发生异常", e);
                }
            });

        } else {
            Thread.ofVirtual().start(() -> {
                try {
                    progressTimer = new Timer(1000, e -> {
                        if (isStart) {
                            downloadProgress.updateSpeed();
                            infoLabel.setText(String.format("<html>已下载: %s | 速度: %s/s</html>",
                                    DownloadProgress.formatSize(downloadProgress.getDownloadedBytes()),
                                    DownloadProgress.formatSize(downloadProgress.getSpeed())));
                        }
                    });
                    progressTimer.start();
                    JProgressBar progressBar = new JProgressBar(0, 100);
                    progressBar.setStringPainted(true);
                    threadProgressBarList.add(progressBar);
                    ProgressBarsPanel.add(progressBar);
                    var isSuccess = URLDownloadTool.singleThreadDownload(url, savePath, fileName, fileSize, 10, progressBar, pauseController, downloadProgress, headers);
                    progressTimer.stop();
                    if (!isSuccess) {
                        downloadControlButton.setEnabled(false);
                        infoLabel.setText("单线程下载异常");
                        JOptionPane.showMessageDialog(this, "下载失败\n单线程下载异常", "错误", JOptionPane.ERROR_MESSAGE);
                        stop();
                    } else {
                        isFinally = true;
                        infoLabel.setText(String.format("<html>下载完成 | 大小: %s</html>", DownloadProgress.formatSize(fileSize)));
                    }
                    downloadControlButton.setEnabled(false);
                    ProgressBarsPanel.removeAll();
                    threadProgressBarList.clear();

                    this.revalidate();
                    this.repaint();
                } catch (Exception e) {
                    if (progressTimer != null) progressTimer.stop();
                    logger.error("单线程下载发生异常", e);
                }
            });
        }
    }

    public void doWhenStop() {
        pauseController.pause();
        if (progressTimer != null) progressTimer.stop();
        infoLabel.setText("<html>已暂停</html>");
    }

    @Override
    public void doWhenExit() {
        if (progressTimer != null) progressTimer.stop();
        threadProgressBarList.clear();
    }

}
