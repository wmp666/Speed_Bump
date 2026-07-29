package com.wmp.downloader.ui.task.bilibili.file;

import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.ConvergenceTool;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.download.URLDownloadTool.DownloadProgress;
import com.wmp.downloader.tools.download.URLDownloadTool.PauseController;
import com.wmp.downloader.tools.ui.ToastMessage;
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
        } else {
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

                SwingUtilities.invokeLater(() -> infoLabel.setText(StringFormat.translate("task", "task.download_task.downloading_video")));
                try {
                    videoSuccess = downloadFile(videoUri, tempDir, "video.m4s", headers, fileSize[0]);
                    //if (!videoSuccess) return;
                } catch (Exception e) {
                    logger.error(StringFormat.translate("task", "task.download_task.video_download_failed"), e);
                    ToastMessage.show(this, StringFormat.translate("task", "task.download_task.video_download_failed"), ToastMessage.ERROR);
                }

                SwingUtilities.invokeLater(() -> infoLabel.setText(StringFormat.translate("task", "task.download_task.downloading_audio")));
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
                    logger.error(StringFormat.translate("task", "task.download_task.audio_download_failed"), e);
                    ToastMessage.show(this, StringFormat.translate("task", "task.download_task.audio_download_failed"), ToastMessage.ERROR);
                }

                //合并文件
                if (videoSuccess && audioSuccess) {
                    ProgressBarsPanel.removeAll();
                    var jProgressBar = new JProgressBar();
                    jProgressBar.setStringPainted(true);
                    ProgressBarsPanel.add(jProgressBar);
                    infoLabel.setText(StringFormat.translate("task", "task.download_task.merging_file"));
                    var isConverged = ConvergenceTool.converge(new File(tempDir, "video.m4s"), new File(tempDir, "audio.m4s"), new File(savePath, fileName), jProgressBar);
                    infoLabel.setText(StringFormat.translate("task", isConverged ? "task.download_task.merge_success" : "task.download_task.merge_failed"));

                    //删除文件
                    DataControl.deleteFolder(tempDir, false);
                }

                isFinally = true;
                downloadControlButton.setEnabled(false);
                ProgressBarsPanel.removeAll();
                SwingUtilities.invokeLater(() -> infoLabel.setText(String.format("<html>%s</html>", StringFormat.translate("task", "task.download_task.download_complete"), DownloadProgress.formatSize(fileSize[0] + fileSize[1]))));
                this.revalidate();
                this.repaint();
            } catch (Exception e) {
                if (progressTimer != null) progressTimer.stop();
                logger.error(StringFormat.translate("task", "task.download_task.download_exception"), e);
                ToastMessage.show(this, String.format(StringFormat.translate("task", "task.download_task.download_failed_detail"), e.getMessage()), ToastMessage.ERROR);
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
                infoLabel.setText(String.format("<html>%s</html>",
                        String.format(StringFormat.translate("task", "task.download_task.progress_single"),
                                DownloadProgress.formatSize(downloadProgress.getDownloadedBytes()),
                                DownloadProgress.formatSize(downloadProgress.getSpeed()))));
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
            infoLabel.setText(StringFormat.translate("task", "task.download_task.single_thread_error"));
            ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_failed_single"), ToastMessage.ERROR);
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
        infoLabel.setText("<html>" + StringFormat.translate("task", "task.download_task.paused") + "</html>");
    }

    @Override
    public void doWhenExit() {
        if (progressTimer != null) progressTimer.stop();
        threadProgressBarList.clear();
    }

}
