package com.wmp.downloader.tools;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.alerts.Alert;
import com.frostwire.jlibtorrent.alerts.AlertType;
import com.frostwire.jlibtorrent.alerts.MetadataReceivedAlert;
import com.frostwire.jlibtorrent.swig.create_torrent;
import com.frostwire.jlibtorrent.swig.entry;
import com.frostwire.jlibtorrent.swig.error_code;
import com.frostwire.jlibtorrent.swig.libtorrent;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

public class MagnetToTorrent {

    private static final Logger logger = Logger.getLogger(MagnetToTorrent.class);

    static void main() {
        System.out.println(magnetToTorrent(
                "magnet:?xt=urn:btih:3a8a09a5aae8aca84d3990ce62a2c664e2ef5892&dn=zh-cn_windows_10_consumer_editions_version_22h2_updated_oct_2025_x64_dvd_38efd00d.iso&xl=7168839680",
                DataControl.getDownloadFilePath().getAbsolutePath(),
                DataControl.getTempPath().getAbsolutePath()));
    }

    public static String magnetToTorrent(String magnetUri, String savePath, String tempPath){
        logger.info("开始转换磁力链接");

        SessionManager manager = new SessionManager();
        manager.start(new SessionParams());
        /*
        String magnetUri = "magnet:?xt=urn:btih:YOUR_INFO_HASH";
        String savePath = "D:/downloads";*/

        AddTorrentParams addParams = AddTorrentParams.parseMagnetUri(magnetUri);
        addParams.savePath(savePath);

        // 添加任务，获取句柄
        TorrentHandle handle = new TorrentHandle(manager.swig().add_torrent(addParams.swig(), new error_code()));
        CountDownLatch latch = new CountDownLatch(1);

        final String[] torrentPath = {null};

        // 监听元数据接收事件
        manager.addListener(new AlertListener() {
            @Override
            public int[] types() {
                return new int[]{AlertType.METADATA_RECEIVED.swig()};
            }

            @Override
            public void alert(Alert<?> alert) {
                if (alert instanceof MetadataReceivedAlert) {
                    logger.info("元数据已接收，正在生成种子文件...");
                    try {
                        // 1. 获取 TorrentInfo 对象
                        TorrentInfo ti = handle.torrentFile();

                        // 2. 创建 create_torrent 对象并生成 entry
                        create_torrent ct = new create_torrent(ti.swig());
                        entry e = ct.generate();

                        // 3. ★ 使用 libtorrent.bencode 将 entry 编码为字节数组 ★
                        byte[] bencodedData = ti.bencode();

                        // 4. 保存为 .torrent 文件
                        String torrentFileName = ti.name() + ".torrent";
                        var file = new File(tempPath + "/" + torrentFileName);
                        torrentPath[0] = file.getAbsolutePath();
                        try (FileOutputStream fos = new FileOutputStream(file)) {
                            fos.write(bencodedData);
                        }
                        logger.info("种子文件已生成: " + torrentFileName);
                        latch.countDown();

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.error(e);
        }
        manager.stop();

        return torrentPath[0];
    }
}