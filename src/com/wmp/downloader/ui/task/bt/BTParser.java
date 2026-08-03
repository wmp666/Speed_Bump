package com.wmp.downloader.ui.task.bt;

import com.frostwire.jlibtorrent.TorrentInfo;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.MagnetToTorrent;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import com.wmp.downloader.ui.task.createTask.LinkFolderInfoPanel;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

public class BTParser extends Parser {

    private static final Logger logger = Logger.getLogger(BTParser.class);

    public static int getLinkMode(String content) {
        if (content.endsWith(".torrent")) {
            return 0;
        } else if (content.startsWith("magnet:")) {
            return 1;
        } else return -1;
    }

    @Override
    public JPanel parse(String content) {
        if (content.endsWith(".torrent")) {
            File file = new File(content);
            TorrentInfo ti = new TorrentInfo(file);
            //单文件
            if (ti.numFiles() == 1) {
                return LinkFileInfoPanel
                        .createBasicLinkFileInfoPanel(ti.name(), ti.totalSize(), "BT-Torrent", content);
            } else if (ti.numFiles() > 1) {
                var fileStorage = ti.files();

                String[] torrentLink = new String[fileStorage.numFiles()];
                Arrays.fill(torrentLink, content);

                String[] fileTypes = new String[fileStorage.numFiles()];
                String[] fileNames = new String[fileStorage.numFiles()];
                long[] fileSizes = new long[fileStorage.numFiles()];
                for (int i = 0; i < fileStorage.numFiles(); i++) {
                    fileNames[i] = fileStorage.fileName(i);
                    fileSizes[i] = fileStorage.fileSize(i);
                    var split = fileNames[i].split("\\.");
                    fileTypes[i] = split.length >= 2 ? split[split.length - 1] : "None";
                }


                return LinkFolderInfoPanel
                        .createBasicLinkFolderInfoPanel(ti.name(),
                                fileSizes, //size
                                "BT-Torrent",
                                torrentLink, //url
                                fileNames, //filesName
                                fileTypes); //fileTypes

            }
        } else if (content.startsWith("magnet:")) {
            var torrentPath = MagnetToTorrent.magnetToTorrent(content, DataControl.getDownloadFilePath().getAbsolutePath(), DataControl.getTempPath().getAbsolutePath());
            File file = new File(torrentPath);
            logger.info("接收到的种子文件位置已生成: " + torrentPath);
            TorrentInfo ti = new TorrentInfo(file);
            //单文件
            if (ti.numFiles() == 1) {
                return LinkFileInfoPanel
                        .createBasicLinkFileInfoPanel(ti.name(), ti.totalSize(), "BT-Magnet", torrentPath);
            } else if (ti.numFiles() > 1) {
                var fileStorage = ti.files();

                String[] torrentLink = new String[fileStorage.numFiles()];
                Arrays.fill(torrentLink, torrentPath);

                String[] fileTypes = new String[fileStorage.numFiles()];
                String[] fileNames = new String[fileStorage.numFiles()];
                long[] fileSizes = new long[fileStorage.numFiles()];
                for (int i = 0; i < fileStorage.numFiles(); i++) {
                    fileNames[i] = fileStorage.fileName(i);
                    fileSizes[i] = fileStorage.fileSize(i);
                    var split = fileNames[i].split("\\.");
                    fileTypes[i] = split.length >= 2 ? split[split.length - 1] : "None";
                }


                return LinkFolderInfoPanel
                        .createBasicLinkFolderInfoPanel(ti.name(),
                                fileSizes, //size
                                "BT-Magnet",
                                torrentLink, //url
                                fileNames, //filesName
                                fileTypes); //fileTypes

            }
        }

        return null;
    }
}
