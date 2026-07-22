package com.wmp.downloader.ui.task.bilibili.folder;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.download.URLDownloadTool.DownloadProgress;
import com.wmp.downloader.tools.download.URLDownloadTool.PauseController;
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

public class BiliFolderDownloadTask extends DownloadTask {

    private static final Logger logger = Logger.getLogger(BiliFolderDownloadTask.class);

    private final String[][] biliUrls;
    private final String[] fileNames;
    private final long[] fileSizes;
    private final long[] videoSizes;
    private final long[] audioSizes;
    private final int threadNum;
    private final long totalFileSize;
    private final int mode;

    private final ArrayList<PauseController> pauseControllerList = new ArrayList<>();
    private final ArrayList<DownloadProgress> downloadProgressList = new ArrayList<>();
    private Timer progressTimer;
    private JTabbedPane fileTabbedPane;
    private AtomicInteger completedCount;
    private volatile boolean hasAnyError = false;

    /**
     * @param folderName    文件夹名称
     * @param totalFileSize 总文件大小
     * @param fileSizes     每个选中分P的大小（视频+音频）
     * @param biliUrls      每个选中分P的下载链接 [分P][0=视频URL, 1=音频URL]
     * @param fileNames     每个选中分P的文件名
     * @param savePath      保存路径
     * @param threadNum     线程数
     * @param mode          下载模式 0=自动 1=单线程
     */
    public BiliFolderDownloadTask(String folderName, long totalFileSize, long[] fileSizes,
                                  long[] videoSizes, long[] audioSizes,
                                  String[][] biliUrls, String[] fileNames,
                                  File savePath, int threadNum, int mode) {
        super(folderName, savePath);
        this.totalFileSize = totalFileSize;
        this.fileSizes = fileSizes;
        this.videoSizes = videoSizes;
        this.audioSizes = audioSizes;
        this.biliUrls = biliUrls;
        this.fileNames = fileNames;
        this.threadNum = threadNum;
        this.mode = mode;

        File tempDir = new File(DataControl.getTempPath(), folderName + ".temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        } else {
            DataControl.deleteFolder(tempDir, false);
            tempDir.mkdirs();
        }
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
        if (pauseControllerList.isEmpty()) {
            for (int i = 0; i < biliUrls.length; i++) {
                pauseControllerList.add(new PauseController());
                downloadProgressList.add(new DownloadProgress());
            }
        } else {
            pauseControllerList.forEach(PauseController::resume);
        }

        ProgressBarsPanel.removeAll();
        fileTabbedPane = new JTabbedPane(JTabbedPane.LEFT, JTabbedPane.SCROLL_TAB_LAYOUT);
        ProgressBarsPanel.add(fileTabbedPane);

        Map<String, String> headers = buildHeaders();
        completedCount = new AtomicInteger(0);
        hasAnyError = false;

        File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
        if (!tempDir.exists()) tempDir.mkdirs();

        for (int i = 0; i < biliUrls.length; i++) {
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

            String videoUrl = biliUrls[i].length > 0 ? biliUrls[i][0] : null;
            String audioUrl = biliUrls[i].length > 1 ? biliUrls[i][1] : null;

            File partTempDir = new File(tempDir, "part_" + fileIndex);
            partTempDir.mkdirs();

            long vSize = fileIndex < videoSizes.length ? videoSizes[fileIndex] : 0;
            long aSize = fileIndex < audioSizes.length ? audioSizes[fileIndex] : 0;

            var ref = new Object() {
                String outputName = String.format("%02d_%s", fileIndex + 1, fName);
            };
            if (!ref.outputName.toLowerCase().endsWith(".mp4")) {
                ref.outputName = ref.outputName + ".mp4";
            }

            Thread.ofVirtual().start(() -> {
                try {
                    downloadAndConvergePart(fileIndex, videoUrl, audioUrl,
                            partTempDir, new File(savePath, fileName), ref.outputName,
                            vSize, aSize, headers, progressPanel);
                } catch (Exception e) {
                    logger.error("文件[" + fName + "]处理异常", e);
                    hasAnyError = true;
                    checkAllFilesCompleted();
                }
            });
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
                        completed, biliUrls.length));
            }
        });
        progressTimer.start();
    }

    private void downloadAndConvergePart(int fileIndex, String videoUrl, String audioUrl,
                                         File partTempDir, File parentDir, String outputName,
                                         long videoSize, long audioSize,
                                         Map<String, String> headers,
                                         JPanel progressPanel) {
        PauseController pc = pauseControllerList.get(fileIndex);
        DownloadProgress dp = downloadProgressList.get(fileIndex);
        pc.resume();

        boolean videoSuccess = false;
        boolean audioSuccess = false;

        try {
            if (videoUrl != null) {
                SwingUtilities.invokeLater(() -> infoLabel.setText("正在下载视频: " + outputName));
                JProgressBar videoProgressBar = new JProgressBar(0, 100);
                videoProgressBar.setStringPainted(true);
                SwingUtilities.invokeLater(() -> progressPanel.add(videoProgressBar));
                SwingUtilities.invokeLater(() -> progressPanel.revalidate());

                videoSuccess = URLDownloadTool.singleThreadDownload(
                        URI.create(videoUrl), partTempDir, "video.m4s",
                        videoSize, 10, videoProgressBar, pc, dp, headers);

                if (!videoSuccess) {
                    logger.error("视频下载失败: " + outputName);
                    hasAnyError = true;
                    checkAllFilesCompleted();
                    return;
                }
            }

            dp.resetSpeed();
            dp.resetMergedBytes();
            PauseController audioPc = new PauseController();
            audioPc.resume();
            pauseControllerList.set(fileIndex, audioPc);
            DownloadProgress audioDp = new DownloadProgress();
            downloadProgressList.set(fileIndex, audioDp);

            if (audioUrl != null) {
                SwingUtilities.invokeLater(() -> infoLabel.setText("正在下载音频: " + outputName));
                JProgressBar audioProgressBar = new JProgressBar(0, 100);
                audioProgressBar.setStringPainted(true);
                SwingUtilities.invokeLater(() -> progressPanel.add(audioProgressBar));
                SwingUtilities.invokeLater(() -> progressPanel.revalidate());

                audioSuccess = URLDownloadTool.singleThreadDownload(
                        URI.create(audioUrl), partTempDir, "audio.m4s",
                        audioSize, 10, audioProgressBar, audioPc, audioDp, headers);

                if (!audioSuccess) {
                    logger.error("音频下载失败: " + outputName);
                    hasAnyError = true;
                    checkAllFilesCompleted();
                    return;
                }
            }

            if (videoSuccess && audioSuccess) {
                SwingUtilities.invokeLater(() -> {
                    infoLabel.setText("正在合并: " + outputName);
                    progressPanel.removeAll();
                    progressPanel.revalidate();
                    progressPanel.repaint();
                });

                JProgressBar mergeProgressBar = new JProgressBar(0, 100);
                mergeProgressBar.setStringPainted(true);
                SwingUtilities.invokeLater(() -> {
                    progressPanel.add(mergeProgressBar);
                    progressPanel.revalidate();
                });

                boolean converged = ConvergenceTool.converge(
                        new File(partTempDir, "video.m4s"),
                        new File(partTempDir, "audio.m4s"),
                        new File(parentDir, outputName),
                        mergeProgressBar);

                if (!converged) {
                    logger.error("合并失败: " + outputName);
                    hasAnyError = true;
                }

                SwingUtilities.invokeLater(() -> {
                    infoLabel.setText("完成: " + outputName);
                    progressPanel.removeAll();
                    progressPanel.revalidate();
                    progressPanel.repaint();
                });

                DataControl.deleteFolder(partTempDir, false);
            }

        } catch (Exception e) {
            logger.error("文件[" + outputName + "]处理异常", e);
            hasAnyError = true;
        } finally {
            checkAllFilesCompleted();
        }
    }
    private void checkAllFilesCompleted() {
        int completed = completedCount.incrementAndGet();
        if (completed >= biliUrls.length) {
            SwingUtilities.invokeLater(() -> {
                if (progressTimer != null) progressTimer.stop();
                ProgressBarsPanel.removeAll();
                downloadControlButton.setEnabled(false);
                isFinally = true;

                File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
                DataControl.deleteFolder(tempDir, false);

                if (hasAnyError) {
                    infoLabel.setText("部分文件下载失败");
                    JOptionPane.showMessageDialog(this, "部分文件下载失败，请检查日志后重试", "错误", JOptionPane.WARNING_MESSAGE);
                } else {
                    infoLabel.setText(String.format("<html>全部下载完成 | 大小: %s | 文件数: %d</html>",
                            DownloadProgress.formatSize(totalFileSize), biliUrls.length));
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

        File tempDir = new File(DataControl.getTempPath(), fileName + ".temp");
        DataControl.deleteFolder(tempDir, false);
    }
}
