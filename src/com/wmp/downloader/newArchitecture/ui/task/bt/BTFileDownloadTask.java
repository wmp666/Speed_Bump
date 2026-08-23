package com.wmp.downloader.newArchitecture.ui.task.bt;


import com.alibaba.fastjson2.JSONObject;
import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.FileDownloadTask;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.StatusTipPanel;
import com.wmp.downloader.newArchitecture.exception.DownloadException;
import com.wmp.downloader.tools.file.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class BTFileDownloadTask extends FileDownloadTask {

    private final String link;

    /**
     * 0-Torrent 1-Magnet
     */
    private final int linkMode;

    private TorrentHandle handle;
    private SessionManager manager;

    private final StatusTipPanel DOWNLOAD_SIZE_PANEL = StatusTipPanel.DOWNLOAD_SIZE_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_SPEED_PANEL = StatusTipPanel.DOWNLOAD_SPEED_CREATOR.create();
    private final StatusTipPanel SHARE_SIZE_PANEL = StatusTipPanel.SHARE_SIZE_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_FAILED_PANEL = StatusTipPanel.DOWNLOAD_FAILED_CREATOR.create();
    private final StatusTipPanel DOWNLOAD_SUCCESS_PANEL = StatusTipPanel.DOWNLOAD_SUCCESS_CREATOR.create();

    public BTFileDownloadTask(JSONObject jsonObject) {
        super(jsonObject);
        this.link = jsonObject.getString("url");
        this.linkMode = BTParser.getLinkMode(link);

        addStatusTips(DOWNLOAD_SIZE_PANEL, DOWNLOAD_SPEED_PANEL, SHARE_SIZE_PANEL);
    }

    @Override
    public void doWhenExit() {
        if (manager != null) {
            manager.stop();
        }else throw new DownloadException("handle为空");
    }

    @Override
    public void doWhenStart() throws Exception {

        removeAllStatusTip();
        addStatusTips(DOWNLOAD_SIZE_PANEL, DOWNLOAD_SPEED_PANEL, SHARE_SIZE_PANEL);

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
                    DOWNLOAD_SIZE_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(status.allTimeDownload()));
                    DOWNLOAD_SPEED_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(status.downloadRate()) + "/s");
                    SHARE_SIZE_PANEL.setText(URLDownloadTool.DownloadProgress.formatSize(status.allTimeUpload()));
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
                removeAllStatusTip();
                addStatusTip(DOWNLOAD_SUCCESS_PANEL);
            } catch (Exception e) {
                removeAllStatusTip();
                addStatusTip(DOWNLOAD_FAILED_PANEL);
            }
        });
    }

    @Override
    public void doWhenRestart() throws Exception {
        if (handle != null) {
            handle.resume();
            manager.resume();
        } else {
            throw new DownloadException("handle值为空");
        }
    }

    @Override
    public void doWhenStop() {
        if (handle != null) {
            handle.pause();
            manager.pause();
        }else throw new DownloadException("handle为空");
    }
}
