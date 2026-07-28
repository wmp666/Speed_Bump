package com.wmp.downloader.ui.task.bt;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.ui.task.DownloadTask;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class TorrentFileDownloadTask extends DownloadTask {

    private String torrentFile;

    private TorrentHandle handle;
    private SessionManager manager;

    public TorrentFileDownloadTask(File savePath, String fileName, String torrentFile) {
        super(fileName, savePath);
        this.torrentFile = torrentFile;

    }

    @Override
    public void doWhenExit() {
        if (handle != null && manager != null) {
            handle.pause();
            manager.stop();
        }
    }

    @Override
    public void doWhenStart() throws Exception {

        if (this.startCount > 0 && handle != null) {
            handle.resume();

            return;
        }


        // 1. 创建 SessionManager（替代了旧的 Session）
        manager = new SessionManager();

        // 2. 启动会话（可传入 SessionParams 进行配置）
        SessionParams params = new SessionParams();
        manager.start(params);

        // 3. 添加下载任务
        File torrentFile = new File(this.torrentFile);
        File saveDir = savePath;

        AddTorrentParams addParams = new AddTorrentParams();
        addParams.savePath(String.valueOf(saveDir));
        // 从种子文件加载 TorrentInfo
        TorrentInfo ti = new TorrentInfo(torrentFile);

        Thread.sleep(10000);
        manager.download(ti, saveDir);

        // 添加任务，返回 TorrentHandle 用于控制该任务
        addParams.torrentInfo(ti);

        var sha1 = ti.infoHashV1();
        this.handle = manager.find(sha1);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setMaximum(100);
        ProgressBarsPanel.add(progressBar);

        // 4. 监听下载进度和完成事件
        CountDownLatch signal = new CountDownLatch(1);
        manager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null; // 接收所有类型的警报
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert instanceof TorrentFinishedAlert) {
                    //System.out.println("下载完成！");
                    signal.countDown();
                } else if (alert instanceof BlockFinishedAlert) {
                    // 获取进度
                    TorrentStatus status = handle.status();
                    int progress = (int) (status.progress() * 100);
                    progressBar.setValue(progress);
                    infoLabel.setText(String.format(
                            StringFormat.translate("task", "task.download_task.bt.downloading"),
                            URLDownloadTool.DownloadProgress.formatSize(status.allTimeDownload()),
                            URLDownloadTool.DownloadProgress.formatSize(status.allTimeUpload()),
                            URLDownloadTool.DownloadProgress.formatSize(status.downloadRate())
                    ));
                    //System.out.println("下载进度: " + progress + "%");
                }
            }
        });

        Thread.ofVirtual().start(() -> {
            try {
                // 5. 等待下载完成
                signal.await();
                manager.stop();

                isFinally = true;
                ProgressBarsPanel.removeAll();
                infoLabel.setText(String.format(
                        StringFormat.translate("task", "task.download_task.download_complete"),
                        URLDownloadTool.DownloadProgress.formatSize(ti.totalSize())
                ));
            } catch (Exception e) {
                infoLabel.setText(StringFormat.translate("task", "task.download_task.download_exception"));
            }
        });
    }

    @Override
    public void doWhenStop() {
        if (handle != null) {
            handle.pause();
        }
    }
}
