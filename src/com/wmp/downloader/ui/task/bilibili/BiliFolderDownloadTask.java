package com.wmp.downloader.ui.task.bilibili;

import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.download.URLDownloadTool.DownloadProgress;
import com.wmp.downloader.tools.download.URLDownloadTool.PauseController;
import com.wmp.downloader.ui.task.DownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BiliFolderDownloadTask extends DownloadTask {

    private static final Logger logger = Logger.getLogger(BiliFolderDownloadTask.class);

    private final URI[] urls;
    private final String[] fileNames;
    private final long[] fileSizes;
    private final int threadNum;
    private final long totalFileSize;
    private final int mode;

    private final ArrayList<PauseController> pauseControllerList = new ArrayList<>();
    private final ArrayList<DownloadProgress> downloadProgressList = new ArrayList<>();
    private Timer progressTimer;
    private JTabbedPane fileTabbedPane;
    private AtomicInteger completedCount;
    private volatile boolean hasAnyError = false;

    public BiliFolderDownloadTask(String folderName, long totalFileSize, long[] fileSizes, URI[] urls, String[] fileNames, File savePath, int threadNum, int mode) {
        super(folderName, savePath);
        this.totalFileSize = totalFileSize;
        this.fileSizes = fileSizes;
        this.urls = urls;
        this.fileNames = fileNames;
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

    private String truncateTabTitle(String name) {
        if (name == null) return "未知文件";
        if (name.length() > 10) {
            int dotIdx = name.lastIndexOf('.');
            if (dotIdx > 0 && name.length() - dotIdx <= 5) {
                return name.substring(0, 7) + "..." + name.substring(dotIdx);
            }
            return name.substring(0, 10) + "...";
        }
        return name;
    }

    public void doWhenStart() throws Exception {
        pauseControllerList.forEach(PauseController::resume);
        pauseControllerList.clear();
        downloadProgressList.clear();
        for (int i = 0; i < urls.length; i++) {
            pauseControllerList.add(new PauseController());
            downloadProgressList.add(new DownloadProgress());
        }

        ProgressBarsPanel.removeAll();
        fileTabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        ProgressBarsPanel.add(fileTabbedPane);

        Map<String, String> headers = buildHeaders();
        completedCount = new AtomicInteger(0);
        hasAnyError = false;

        for (int i = 0; i < urls.length; i++) {
            final int fileIndex = i;
            String fName = fileIndex < fileNames.length ? fileNames[fileIndex] : "file_" + fileIndex;
            long fSize = fileIndex < fileSizes.length ? fileSizes[fileIndex] : 0;

            JPanel filePanel = new JPanel(new BorderLayout(2, 2));
            JLabel fileLabel = new JLabel(fName);
            fileLabel.putClientProperty("FlatLaf.style", "font: $h3.font");
            filePanel.add(fileLabel, BorderLayout.NORTH);

            JPanel progressPanel = new JPanel(new GridLayout(0, 1, 2, 2));
            filePanel.add(progressPanel, BorderLayout.CENTER);

            fileTabbedPane.addTab(truncateTabTitle(fName), filePanel);

            PauseController pc = pauseControllerList.get(fileIndex);
            DownloadProgress dp = downloadProgressList.get(fileIndex);
            dp.resetSpeed();

            downloadFile(headers, urls[fileIndex], (i + 1) + "：" + fName, fSize, fileIndex, progressPanel);
        }

        progressTimer = new Timer(1000, e -> {
            if (isStart) {
                long totalDownloaded = 0;
                long totalSpeed = 0;
                for (DownloadProgress dp : downloadProgressList) {
                    totalDownloaded += dp.getDownloadedBytes();
                    totalSpeed += dp.getSpeed();
                }
                int completed = completedCount.get();
                infoLabel.setText(String.format("<html>已下载: %s | 速度: %s/s | 文件: %d/%d</html>",
                        DownloadProgress.formatSize(totalDownloaded),
                        DownloadProgress.formatSize(totalSpeed),
                        completed, urls.length));
            }
        });
        progressTimer.start();
    }

    private void downloadFile(Map<String, String> headers, URI url, String fileName, long fileSize, int fileIndex, JPanel progressPanel) throws Exception {
        PauseController pc = pauseControllerList.get(fileIndex);
        DownloadProgress dp = downloadProgressList.get(fileIndex);

        if (mode == 0 && fileSize > 0 && URLDownloadTool.isCanUseMultithreading(url, fileSize, headers)) {
            ArrayList<JProgressBar> fileProgressBars = new ArrayList<>();
            for (var i = 0; i < threadNum; i++) {
                var progressBar = new JProgressBar(0, 100);
                progressBar.setStringPainted(true);
                fileProgressBars.add(progressBar);
                progressPanel.add(progressBar);
            }

            Thread.ofVirtual().start(() -> {
                try {
                    var downloadingInfo = URLDownloadTool.download(url, savePath, fileName, fileSize, threadNum, 10, fileProgressBars, pc, dp, headers);

                    var latch = downloadingInfo.latch();
                    var executor = downloadingInfo.executor();
                    var tasks = downloadingInfo.downloadTasks();



                    latch.await();
                    executor.shutdown();

                    boolean hasError = false;
                    for (URLDownloadTool.DownloadTaskRunnable task : tasks) {
                        if (task.hasError()) {
                            hasError = true;
                            break;
                        }
                    }

                    if (hasError) {
                        logger.error("文件[" + fileName + "]部分分段下载失败");
                        hasAnyError = true;
                    } else {
                        SwingUtilities.invokeLater(() -> infoLabel.setText("正在合并: " + fileName));
                        JProgressBar mergeProgressBar = new JProgressBar(0, 100);
                        mergeProgressBar.setStringPainted(true);
                        progressPanel.add(mergeProgressBar);
                        progressPanel.revalidate();

                        dp.resetMergedBytes();
                        URLDownloadTool.mergeParts(new File(savePath, this.fileName), fileName, threadNum, fileSize, mergeProgressBar, pc, dp);
                    }
                } catch (Exception e) {
                    logger.error("文件[" + fileName + "]下载异常", e);
                    hasAnyError = true;
                } finally {
                    checkAllFilesCompleted();
                }
            });
        } else {

            JProgressBar progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressPanel.add(progressBar);
            progressPanel.revalidate();
            Thread.ofVirtual().start(() -> {
                try {
                    URLDownloadTool.singleThreadDownload(url, new File(savePath, this.fileName), fileName, fileSize, 10, progressBar, pc, dp, headers);
                } catch (Exception e) {
                    logger.error("文件[" + fileName + "]单线程下载异常", e);
                    hasAnyError = true;
                } finally {
                    checkAllFilesCompleted();
                }
            });
        }
    }

    private void checkAllFilesCompleted() {
        int completed = completedCount.incrementAndGet();
        if (completed >= urls.length) {
            SwingUtilities.invokeLater(() -> {
                if (progressTimer != null) progressTimer.stop();
                ProgressBarsPanel.removeAll();
                downloadControlButton.setEnabled(false);
                isFinally = true;
                if (hasAnyError) {
                    infoLabel.setText("部分文件下载失败");
                    JOptionPane.showMessageDialog(this, "部分文件下载失败，请检查日志后重试", "错误", JOptionPane.WARNING_MESSAGE);
                } else {
                    infoLabel.setText(String.format("<html>全部下载完成 | 大小: %s | 文件数: %d</html>",
                            DownloadProgress.formatSize(totalFileSize), urls.length));
                }
                this.revalidate();
                this.repaint();
            });
        }
    }

    public void doWhenStop() {
        pauseControllerList.forEach(PauseController::pause);
        if (progressTimer != null) progressTimer.stop();
        infoLabel.setText("<html>已暂停</html>");
    }

    @Override
    public void doWhenExit() {
        if (progressTimer != null) progressTimer.stop();
        pauseControllerList.clear();
        downloadProgressList.clear();
    }

}
