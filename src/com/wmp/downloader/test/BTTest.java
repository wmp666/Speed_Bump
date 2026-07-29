package com.wmp.downloader.test;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert;
import com.wmp.downloader.tools.download.URLDownloadTool;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class BTTest {
    static void main() throws InterruptedException {
        // 1. 创建 SessionManager（替代了旧的 Session）
        SessionManager manager = new SessionManager();

        // 2. 启动会话（可传入 SessionParams 进行配置）
        SessionParams params = new SessionParams();
        manager.start(params);

        // 3. 添加下载任务
        File torrentFile = new File("E:/Users/21348/Downloads/[DL] Escape the Backrooms [P] [RUS + ENG + 7 ENG] (2025, Horror) (1.2510) [Portable] [rutracker-6247724].torrent");
        File saveDir = new File("D:/torrentDownloads");

        AddTorrentParams addParams = new AddTorrentParams();
        addParams.savePath(String.valueOf(saveDir));
        // 从种子文件加载 TorrentInfo
        TorrentInfo ti = new TorrentInfo(torrentFile);
        System.out.println(ti.name());
        System.out.println(URLDownloadTool.DownloadProgress.formatSize(ti.totalSize()));

        Thread.sleep(10000);
        manager.download(ti, saveDir);


        // 添加任务，返回 TorrentHandle 用于控制该任务
        addParams.torrentInfo(ti);

        var sha1 = ti.infoHashV1();
        System.out.println(sha1);
        TorrentHandle handle = manager.find(sha1);


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
                    System.out.println("下载完成！");
                    signal.countDown();
                } else if (alert instanceof BlockFinishedAlert) {
                    // 获取进度
                    TorrentStatus status = handle.status();
                    int progress = (int) (status.progress() * 100);
                    System.out.println("下载进度: " + progress + "%");
                }
            }
        });

        // 5. 等待下载完成
        signal.await();
        manager.stop();
    }
}
