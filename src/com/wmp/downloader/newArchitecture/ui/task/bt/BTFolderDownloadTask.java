package com.wmp.downloader.newArchitecture.ui.task.bt;


import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.frostwire.jlibtorrent.swig.error_code;
import com.wmp.downloader.newArchitecture.abstractTask.downloadTask.FolderDownloadTask;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.download.URLDownloadTool;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;

import javax.swing.*;
import java.io.File;
import java.util.concurrent.CountDownLatch;

public class BTFolderDownloadTask extends FolderDownloadTask {
    private final boolean[] chooseFiles;
    private final String torrentFilePath;
    private final long chooseSize;

    private TorrentHandle handle;
    private SessionManager manager;

    public BTFolderDownloadTask(JSONObject jsonObject) {
        super(jsonObject);
        JSONArray statusArray = jsonObject.getJSONArray("selectedStatus");
        boolean[] chooseFiles = new boolean[statusArray.size()];
        for (int i = 0; i < statusArray.size(); i++) {
            chooseFiles[i] = statusArray.getBoolean(i); // 直接取布尔值
        }
        this.chooseFiles = chooseFiles;
        this.torrentFilePath = jsonObject.getString("url");
        this.chooseSize = jsonObject.getLongValue("size", 0);
    }

    @Override
    public void doWhenExit() {
        if (handle != null && manager != null) {
            handle.saveResumeData();
            handle.pause();
            manager.stop();
        }
    }

    @Override
    public void doWhenStart() throws Exception {


        // 1. 创建并启动会话
        manager = new SessionManager();
        SessionParams params = new SessionParams();
        manager.start(params);

        // 2. 加载种子信息
        TorrentInfo ti = new TorrentInfo(new File(this.torrentFilePath));

        // 3. 检查目标文件夹是否存在
        File targetDir = new File(savePath, ti.name());
        if (targetDir.exists()) {
            int option = JOptionPane.showConfirmDialog(null,
                    StringFormat.translate("task", "task.download_task.delete_exists_file.confirm"),
                    "确认", JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                DataControl.deleteFolder(targetDir, true);
            } else {
                isStart = false;
                return; // 用户取消
            }
        }

        // 4. 配置 AddTorrentParams
        AddTorrentParams addParams = new AddTorrentParams();
        addParams.savePath(savePath.getAbsolutePath());
        addParams.torrentInfo(ti);

        // 设置文件优先级
        Priority[] priorities = new Priority[ti.numFiles()];
        for (int i = 0; i < ti.numFiles(); i++) {
            priorities[i] = chooseFiles[i] ? Priority.NORMAL : Priority.IGNORE;
        }
        addParams.filePriorities(priorities);

        // 5. 添加任务并获取句柄
        this.handle = new TorrentHandle(manager.swig().add_torrent(addParams.swig(), new error_code()));
        handle.renameFile(0, fileName);
        if (handle == null || !handle.isValid()) {
            ToastMessage.show(null, StringFormat.translate("task", "task.download_task.download_exception"), ToastMessage.ERROR);
            return;
        }

        // 6. 创建进度条
        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(false);
        progressBar.setMaximum(100);
        ProgressBarsPanel.add(UITools.createProgressBarsPanel(
                UITools.createProgressBarPanel(progressBar)));

        // 7. 等待下载完成
        CountDownLatch signal = new CountDownLatch(1);

        // 用于限流 UI 更新的变量
        final long[] lastUpdateTime = {0};
        final int[] lastProgress = {-1};

        // 8. 添加警报监听器
        manager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return null; // 监听所有类型
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert instanceof TorrentFinishedAlert) {
                    signal.countDown();
                } else if (alert instanceof BlockFinishedAlert) {
                    // 检查句柄有效性
                    if (handle == null || !handle.isValid()) {
                        return;
                    }

                    TorrentStatus status = handle.status();
                    int progress = (int) (status.progress() * 100);

                    lastProgress[0] = progress;

                    // 在 UI 线程中更新组件
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(progress);
                        infoLabel.setText(String.format(
                                StringFormat.translate("task", "task.download_task.bt.downloading"),
                                URLDownloadTool.DownloadProgress.formatSize(status.allTimeDownload()),
                                URLDownloadTool.DownloadProgress.formatSize(BTFolderDownloadTask.this.chooseSize),
                                URLDownloadTool.DownloadProgress.formatSize(status.allTimeUpload()),
                                URLDownloadTool.DownloadProgress.formatSize(status.downloadRate())
                        ));
                    });
                } else if (alert instanceof com.frostwire.jlibtorrent.alerts.TorrentErrorAlert) {
                    // 捕获错误并提示
                    com.frostwire.jlibtorrent.alerts.TorrentErrorAlert errorAlert =
                            (com.frostwire.jlibtorrent.alerts.TorrentErrorAlert) alert;
                    SwingUtilities.invokeLater(() -> {
                        ToastMessage.show(null, StringFormat.translate("task", "task.download_task.download_exception") + "：" + errorAlert.message(), ToastMessage.ERROR);
                    });
                }
            }
        });

        // 9. 启动虚拟线程等待完成
        Thread.ofVirtual().start(() -> {
            try {
                signal.await();
                manager.stop();
                isFinally = true;
                ProgressBarsPanel.removeAll();
                SwingUtilities.invokeLater(() -> {
                    infoLabel.setText(String.format(
                            StringFormat.translate("task", "task.download_task.download_complete"),
                            URLDownloadTool.DownloadProgress.formatSize(ti.totalSize())
                    ));
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    infoLabel.setText(StringFormat.translate("task", "task.download_task.download_exception"));
                });
            }
        });
    }

    @Override
    public void doWhenRestart() throws Exception {
        // 如果是恢复下载
        if (handle != null && handle.isValid()) {
            handle.resume();
        } else {
            throw new NullPointerException("handle值为空");
        }
    }

    @Override
    public void doWhenStop() {
        if (handle != null) {
            handle.saveResumeData();
            handle.pause();
        }
    }
}
