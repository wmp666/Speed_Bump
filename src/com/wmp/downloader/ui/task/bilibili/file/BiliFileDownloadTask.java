package com.wmp.downloader.ui.task.bilibili.file;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.download.URLDownloadTool.DownloadProgress;
import com.wmp.downloader.tools.download.URLDownloadTool.PauseController;
import com.wmp.downloader.ui.task.DownloadTask;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class BiliFileDownloadTask extends DownloadTask {

    private static final Logger logger = Logger.getLogger(BiliFileDownloadTask.class);

    private final String[] url;
    private final int threadNum;
    private final long[] fileSize;
    private final int mode;
    private final ArrayList<JProgressBar> threadProgressBarList = new ArrayList<>();
    private PauseController pauseController = new PauseController();
    private DownloadProgress downloadProgress = new DownloadProgress();
    private Timer progressTimer;

    public BiliFileDownloadTask(String fileName, long[] fileSize, String[] url, File savePath, int threadNum, int mode) {
        super(fileName, savePath);
        this.fileSize = fileSize;
        this.url = url;
        this.threadNum = threadNum;
        this.mode = mode;

        //删除将要用于存入数据的文件夹
        File tempDir = new File(savePath, fileName + ".temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }else{
           DataControl.deleteFolder(tempDir, false);
        }
    }

    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", "https://www.bilibili.com");
        headers.put("Sec-Fetch-Mode", "no-cors");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36");
        return headers;
    }

    public void doWhenStart() throws Exception {
        pauseController = new PauseController();
        downloadProgress = new DownloadProgress();

        ProgressBarsPanel.removeAll();
        threadProgressBarList.clear();

        File tempDir = new File(savePath, fileName + ".temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        Map<String, String> headers = buildHeaders();
        URI videoUri = URI.create(url[0]);
        URI audioUri = URI.create(url[1]);

        Thread.ofVirtual().start(() -> {
            try {

                boolean videoSuccess = false;
                boolean audioSuccess = false;

                SwingUtilities.invokeLater(() -> infoLabel.setText("正在下载视频..."));
                try {
                    videoSuccess = downloadFile(videoUri, tempDir, "video.m4s", headers, fileSize[0]);
                    //if (!videoSuccess) return;
                } catch (Exception e) {
                    logger.error("视频下载失败", e);
                    JOptionPane.showMessageDialog(this, "视频下载失败", "错误", JOptionPane.ERROR_MESSAGE);
                }

                SwingUtilities.invokeLater(() -> infoLabel.setText("正在下载音频..."));
                downloadProgress.resetSpeed();
                downloadProgress.resetMergedBytes();
                ProgressBarsPanel.removeAll();
                threadProgressBarList.clear();

                pauseController = new PauseController();
                downloadProgress = new DownloadProgress();
                pauseController.resume();

                try {
                    audioSuccess = downloadFile(audioUri, tempDir, "audio.m4s", headers, fileSize[1]);
                    //if (!audioSuccess) return;
                } catch (Exception e) {
                    logger.error("音频下载失败", e);
                    JOptionPane.showMessageDialog(this, "音频下载失败", "错误", JOptionPane.ERROR_MESSAGE);
                }

                //合并文件
                if (videoSuccess && audioSuccess){
                    ProgressBarsPanel.removeAll();
                    var jProgressBar = new JProgressBar();
                    jProgressBar.setStringPainted(true);
                    ProgressBarsPanel.add(jProgressBar);
                    infoLabel.setText("正在合并文件...");
                    var isConverged = ConvergenceTool.converge(new File(tempDir, "video.m4s"), new File(tempDir, "audio.m4s"), new File(savePath, fileName), jProgressBar);
                    infoLabel.setText("合并" + (isConverged? "成功": "失败"));

                    //删除文件
                    DataControl.deleteFolder(tempDir, false);
                }

                isFinally = true;
                downloadControlButton.setEnabled(false);
                ProgressBarsPanel.removeAll();
                SwingUtilities.invokeLater(() -> infoLabel.setText(String.format("<html>下载完成 | 大小: %s</html>", DownloadProgress.formatSize(fileSize[0] + fileSize[1]))));
                this.revalidate();
                this.repaint();
            } catch (Exception e) {
                if (progressTimer != null) progressTimer.stop();
                logger.error("下载发生异常", e);
                JOptionPane.showMessageDialog(this, "下载失败\n" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                isStart = false;
            }
        });
    }

    private boolean downloadFile(URI uri, File tempDir, String targetFileName, Map<String, String> headers, long size) throws Exception {
        return downloadSingleThread(uri, tempDir, targetFileName, headers, size);
    }

    private boolean downloadSingleThread(URI uri, File tempDir, String targetFileName, Map<String, String> headers, long size) throws Exception {
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

        boolean isSuccess = URLDownloadTool.singleThreadDownload(uri, tempDir, targetFileName, size, 10, progressBar, pauseController, downloadProgress, headers);
        progressTimer.stop();

        if (!isSuccess) {
            downloadControlButton.setEnabled(false);
            infoLabel.setText("单线程下载异常");
            JOptionPane.showMessageDialog(this, "下载失败\n单线程下载异常", "错误", JOptionPane.ERROR_MESSAGE);
            stop();
            return false;
        }

        isStart = false;
        ProgressBarsPanel.removeAll();
        threadProgressBarList.clear();
        return true;
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
