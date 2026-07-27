package com.wmp.downloader.ui.task.douyin;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.download.URLDownloadTool.DownloadProgress;
import com.wmp.downloader.tools.download.URLDownloadTool.PauseController;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.task.DownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DouyinImageDownloadTask extends DownloadTask {

    private static final Logger logger = Logger.getLogger(DouyinImageDownloadTask.class);

    private final String[] imageUrls;          // 每张图片的下载链接
    private final String[] imageNames;         // 每张图片的文件名
    private final long[] imageSizes;           // 每张图片的文件大小（字节）
    private final int threadNum;               // 每张图片的分块线程数
    private final int mode;                    // 0=自动（支持多线程则用多线程），1=强制单线程
    private final long totalFileSize;          // 所有图片总大小

    private final ArrayList<PauseController> pauseControllerList = new ArrayList<>();
    private final ArrayList<DownloadProgress> downloadProgressList = new ArrayList<>();
    private Timer progressTimer;
    private JTabbedPane fileTabbedPane;
    private AtomicInteger completedCount;
    private volatile boolean hasAnyError = false;

    /**
     * 构造器
     * @param folderName    文件夹名称（将作为父目录名）
     * @param imageUrls     图片下载链接数组
     * @param imageNames    图片文件名数组（需含扩展名，如 .jpg）
     * @param imageSizes    图片文件大小数组（字节）
     * @param savePath      保存根目录
     * @param threadNum     每张图片的分块线程数（当 mode=0 且服务器支持 Range 时生效）
     * @param mode          下载模式：0=自动（优先多线程），1=单线程
     */
    public DouyinImageDownloadTask(String folderName, String[] imageUrls, String[] imageNames,
                                   long[] imageSizes, File savePath, int threadNum, int mode) {
        super(folderName, savePath);
        this.imageUrls = imageUrls;
        this.imageNames = imageNames;
        this.imageSizes = imageSizes;
        this.threadNum = threadNum;
        this.mode = 1;

        // 计算总大小
        long total = 0;
        for (long s : imageSizes) total += s;
        this.totalFileSize = total;

        // 创建临时目录（用于存放分块文件）
        File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        } else {
            DataControl.deleteFolder(tempDir, false);
            tempDir.mkdirs();
        }
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", "https://www.douyin.com");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        return headers;
    }

    private String truncateTabTitle(String name) {
        if (name == null) return StringFormat.translate("task", "task.download_task.unknown_file");
        if (name.length() > 10) {
            int dotIdx = name.lastIndexOf('.');
            if (dotIdx > 0 && name.length() - dotIdx <= 5) {
                return name.substring(0, 7) + "..." + name.substring(dotIdx);
            }
            return name.substring(0, 10) + "...";
        }
        return name;
    }

    @Override
    public void doWhenStart() throws Exception {
        // 初始化暂停控制器和进度对象
        if (pauseControllerList.isEmpty()) {
            for (int i = 0; i < imageUrls.length; i++) {
                pauseControllerList.add(new PauseController());
                downloadProgressList.add(new DownloadProgress());
            }
        } else {
            pauseControllerList.forEach(PauseController::resume);
        }

        // UI 初始化：选项卡面板
        ProgressBarsPanel.removeAll();
        fileTabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        ProgressBarsPanel.add(fileTabbedPane);

        Map<String, String> headers = buildHeaders();
        completedCount = new AtomicInteger(0);
        hasAnyError = false;

        File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
        if (!tempDir.exists()) tempDir.mkdirs();

        // 为每张图片启动一个虚拟线程并发下载
        for (int i = 0; i < imageUrls.length; i++) {
            final int index = i;
            String imgName = imageNames[index];
            long imgSize = imageSizes[index];

            // 创建该图片的 UI 面板
            JPanel filePanel = new JPanel(new BorderLayout(2, 2));
            JLabel fileLabel = new JLabel(imgName);
            fileLabel.putClientProperty("FlatLaf.style", "font: $h3.font");
            filePanel.add(fileLabel, BorderLayout.NORTH);

            JPanel progressPanel = new JPanel(new GridLayout(0, 1, 2, 2));
            filePanel.add(progressPanel, BorderLayout.CENTER);

            fileTabbedPane.addTab(truncateTabTitle(imgName), filePanel);

            // 每个图片独立存储分块的临时子目录
            File partTempDir = new File(tempDir, "part_" + index);
            partTempDir.mkdirs();

            Thread.ofVirtual().start(() -> {
                try {
                    downloadImage(index, imageUrls[index], partTempDir,
                            new File(savePath, fileName), imgName,
                            imgSize, headers, progressPanel);
                } catch (Exception e) {
                    logger.error("图片[" + imgName + "]下载异常", e);
                    hasAnyError = true;
                    checkAllFilesCompleted();
                }
            });
        }

        // 定时器更新总进度信息
        progressTimer = new Timer(1000, e -> {
            if (isStart) {
                long totalDownloaded = 0;
                long totalSpeed = 0;
                for (DownloadProgress dp : downloadProgressList) {
                    totalDownloaded += dp.getDownloadedBytes();
                    totalSpeed += dp.getSpeed();
                }
                int completed = completedCount.get();
                infoLabel.setText(String.format(StringFormat.translate("task", "task.download_task.progress_folder"),
                        DownloadProgress.formatSize(totalDownloaded),
                        DownloadProgress.formatSize(totalSpeed),
                        completed, imageUrls.length));
            }
        });
        progressTimer.start();
    }

    /**
     * 下载单张图片（支持多线程分块或单线程）
     */
    private void downloadImage(int index, String url, File tempDir, File parentDir,
                               String fileName, long fileSize,
                               Map<String, String> headers, JPanel progressPanel) {
        PauseController pc = pauseControllerList.get(index);
        DownloadProgress dp = downloadProgressList.get(index);
        pc.resume();
        dp.resetSpeed();
        dp.resetMergedBytes();

        try {
            URI uri = URI.create(url);

            // 判断是否使用多线程（mode=0且服务器支持Range）
            boolean useMultithread = (mode == 0 && URLDownloadTool.isCanUseMultithreading(uri, fileSize));

            if (useMultithread) {
                // ----- 多线程分块下载 -----
                SwingUtilities.invokeLater(() ->
                        infoLabel.setText(String.format("正在多线程下载图片: %s", fileName)));

                // 为每个线程创建进度条
                ArrayList<JProgressBar> threadBars = new ArrayList<>();
                for (int t = 0; t < threadNum; t++) {
                    JProgressBar bar = new JProgressBar(0, 100);
                    bar.setStringPainted(true);
                    threadBars.add(bar);
                    SwingUtilities.invokeLater(() -> {
                        progressPanel.add(bar);
                        progressPanel.revalidate();
                    });
                }

                // 调用多线程下载
                var downloadInfo = URLDownloadTool.download(
                        uri, tempDir, fileName + ".tmp", fileSize,
                        threadNum, 10, threadBars, pc, dp, headers
                );

                var latch = downloadInfo.latch();
                var executor = downloadInfo.executor();
                var tasks = downloadInfo.downloadTasks();

                // 等待所有分块完成
                latch.await();
                executor.shutdown();

                // 检查是否有分块失败
                boolean hasError = false;
                for (var task : tasks) {
                    if (task.hasError()) {
                        hasError = true;
                        break;
                    }
                }
                if (hasError) {
                    logger.error("图片分块下载失败: " + fileName);
                    hasAnyError = true;
                    // 清理临时分块
                    URLDownloadTool.deletePartFiles(fileName + ".tmp", new JProgressBar());
                    return;
                }

                // 合并分块到最终文件
                SwingUtilities.invokeLater(() -> {
                    infoLabel.setText(String.format("正在合并图片: %s", fileName));
                    progressPanel.removeAll();
                    JProgressBar mergeBar = new JProgressBar(0, 100);
                    mergeBar.setStringPainted(true);
                    progressPanel.add(mergeBar);
                    progressPanel.revalidate();
                });

                URLDownloadTool.mergeParts(
                        tempDir, fileName, threadNum, fileSize,
                        (JProgressBar) progressPanel.getComponent(0), pc, dp
                );

                // 移动合并后的文件到目标目录
                File mergedFile = new File(tempDir, fileName);
                File destFile = new File(parentDir, fileName);
                if (mergedFile.exists()) {
                    if (!destFile.getParentFile().exists()) destFile.getParentFile().mkdirs();
                    if (destFile.exists()) destFile.delete();
                    if (mergedFile.renameTo(destFile)) {
                        logger.debug("图片移动成功: " + destFile.getAbsolutePath());
                    } else {
                        // 若重命名失败，尝试拷贝
                        java.nio.file.Files.copy(mergedFile.toPath(), destFile.toPath());
                        mergedFile.delete();
                    }
                }

                // 清理临时分块文件
                URLDownloadTool.deletePartFiles(fileName, new JProgressBar());

            } else {
                // ----- 单线程下载 -----
                SwingUtilities.invokeLater(() ->
                        infoLabel.setText(String.format("正在单线程下载图片: %s", fileName)));

                JProgressBar singleBar = new JProgressBar(0, 100);
                singleBar.setStringPainted(true);
                SwingUtilities.invokeLater(() -> {
                    progressPanel.add(singleBar);
                    progressPanel.revalidate();
                });

                boolean success = URLDownloadTool.singleThreadDownload(
                        uri, parentDir, fileName, fileSize,
                        10, singleBar, pc, dp, headers
                );

                if (!success) {
                    logger.error("单线程下载图片失败: " + fileName);
                    hasAnyError = true;
                    return;
                }
            }

            // 下载成功，更新UI
            SwingUtilities.invokeLater(() -> {
                infoLabel.setText(String.format("图片下载完成: %s", fileName));
                progressPanel.removeAll();
                progressPanel.revalidate();
                progressPanel.repaint();
            });

        } catch (Exception e) {
            logger.error("图片下载异常: " + fileName, e);
            hasAnyError = true;
        } finally {
            checkAllFilesCompleted();
        }
    }

    /**
     * 检查是否所有图片都处理完毕
     */
    private void checkAllFilesCompleted() {
        int completed = completedCount.incrementAndGet();
        if (completed >= imageUrls.length) {
            SwingUtilities.invokeLater(() -> {
                if (progressTimer != null) progressTimer.stop();
                ProgressBarsPanel.removeAll();
                downloadControlButton.setEnabled(false);
                isFinally = true;

                // 删除临时目录
                File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
                DataControl.deleteFolder(tempDir, false);

                if (hasAnyError) {
                    infoLabel.setText(StringFormat.translate("task", "task.download_task.partial_failed"));
                    ToastMessage.show(this, StringFormat.translate("task", "task.download_task.partial_failed"), ToastMessage.ERROR);
                } else {
                    infoLabel.setText(String.format("<html>%s</html>",
                            String.format(StringFormat.translate("task", "task.download_task.progress_single"),
                                    DownloadProgress.formatSize(totalFileSize),
                                    DownloadProgress.formatSize(0))));
                }
                this.revalidate();
                this.repaint();
            });
        }
    }

    @Override
    public void doWhenStop() {
        pauseControllerList.forEach(PauseController::pause);
        if (progressTimer != null) progressTimer.stop();
        infoLabel.setText(StringFormat.translate("task", "task.download_task.paused"));
    }

    @Override
    public void doWhenExit() {
        if (progressTimer != null) progressTimer.stop();
        pauseControllerList.clear();
        downloadProgressList.clear();

        File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
        DataControl.deleteFolder(tempDir, false);
    }
}