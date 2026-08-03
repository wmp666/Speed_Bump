package com.wmp.downloader.ui.task.bt;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.task.DownloadTask;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class BTFileDownloadTask extends DownloadTask {

    private final String link;

    /**
     * 0-Torrent 1-Magnet
     */
    private final int linkMode;

    private TorrentHandle handle;
    private SessionManager manager;

    public BTFileDownloadTask(File savePath, String fileName, String link) {
        super(fileName, savePath);
        this.link = link;
        this.linkMode = BTParser.getLinkMode(link);
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


        // 1. 创建 SessionManager（替代了旧的 Session）
        manager = new SessionManager();

        // 2. 启动会话（可传入 SessionParams 进行配置）
        SessionParams params = new SessionParams();
        manager.start(params);

        // 3. 创建下载任务，同时获取相关信息
        TorrentInfo ti;
        AddTorrentParams addParams = new AddTorrentParams();
        addParams.savePath(savePath.getAbsolutePath());
        if (linkMode == 0 || linkMode == 1) {
            File torrentFile = new File(this.link);
            ti = new TorrentInfo(torrentFile);

            // 添加任务，返回 TorrentHandle 用于控制该任务
            addParams.torrentInfo(ti);
        } else {
            ti = null;
            ToastMessage.show(this, StringFormat.translate("task", "task.download_task.download_exception"), ToastMessage.ERROR);
            return;
        }

        var file = new File(savePath, ti.name());
        if (file.exists()) {
            if (JOptionPane.showConfirmDialog(null,
                    StringFormat.translate("task", "task.download_task.delete_exists_file.confirm")) == JOptionPane.YES_OPTION) {
                DataControl.delete(file, true);
            } else {
                isStart = false;
                return;
            }
        }

        //开始下载
        manager.download(ti, savePath);


        var sha1 = ti.infoHashV1();
        this.handle = manager.find(sha1);
        handle.renameFile(0, fileName);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(false);
        progressBar.setMaximum(100);
        ProgressBarsPanel.add(UITools.createProgressBarPanel(progressBar));

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
                            URLDownloadTool.DownloadProgress.formatSize(ti.totalSize()),
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
    public void doWhenRestart() throws Exception {
        if (handle != null) {
            handle.resume();
        } else {
            throw new NullPointerException("handle值为空");
        }
    }

    @Override
    public void doWhenStop() {
        if (handle != null) {
            handle.pause();
        }
    }
}
