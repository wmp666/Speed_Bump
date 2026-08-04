package com.wmp.downloader.newArchitecture.ui.task.bt;

import com.alibaba.fastjson2.JSONObject;
import com.frostwire.jlibtorrent.TorrentInfo;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.LinkFileInfoPanel;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.LinkFolderInfoPanel;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.MagnetToTorrent;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Arrays;

public class BTParser extends AbstractParser {

    private static final Logger logger = Logger.getLogger(BTParser.class);

    @Override
    public String getID() {
        return "BT";
    }

    @Override
    public String getSupportTip() {
        return "Torrent/Magnet";
    }

    @Override
    protected void updateLinkInfo(String link) {}

    @Override
    protected AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info) {
        if (link.endsWith(".torrent")) {
            File file = new File(link);
            TorrentInfo ti = new TorrentInfo(file);
            //单文件
            if (ti.numFiles() == 1) {
                return LinkFileInfoPanel
                        .createPanel(ti.name(), ti.totalSize(),
                                "BT-Torrent", link, info);
            } else if (ti.numFiles() > 1) {
                var fileStorage = ti.files();

                String[] torrentLink = new String[fileStorage.numFiles()];
                Arrays.fill(torrentLink, link);

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
                        .createPanel(ti.name(),
                                fileSizes, //size
                                "BT-Torrent",
                                torrentLink, //url
                                fileNames, //filesName
                                fileTypes, info); //fileTypes

            }
        } else if (link.startsWith("magnet:")) {
            var torrentPath = MagnetToTorrent.magnetToTorrent(link, DataControl.getDownloadFilePath().getAbsolutePath(), DataControl.getTempPath().getAbsolutePath());
            File file = new File(torrentPath);
            logger.info("接收到的种子文件位置已生成: " + torrentPath);
            TorrentInfo ti = new TorrentInfo(file);
            //单文件
            if (ti.numFiles() == 1) {
                return LinkFileInfoPanel
                        .createPanel(ti.name(), ti.totalSize(),
                                "BT-Magnet", torrentPath, info);
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
                        .createPanel(ti.name(),
                                fileSizes, //size
                                "BT-Magnet",
                                torrentLink, //url
                                fileNames, //filesName
                                fileTypes, info); //fileTypes

            }
        }

        return null;
    }

    @Override
    public boolean isMeetRequirements(String link) {
        return link.strip().endsWith(".torrent") || link.strip().startsWith("magnet:");
    }

    @Override
    protected AbstractTask getTask(String link, JSONObject infoJson) {
        if (infoJson.getIntValue("linkStyle") == 0) {
            return new BTFileDownloadTask(infoJson);
        } else if (infoJson.getIntValue("linkStyle") == 1){
            return new BTFolderDownloadTask(infoJson);
        }
        return null;
    }

    @Override
    public AbstractSpecialSettingsPage getSettingsPage() {
        return null;
    }

    public static int getLinkMode(String content) {
        if (content.endsWith(".torrent")) {
            return 0;
        } else if (content.startsWith("magnet:")) {
            return 1;
        } else return -1;
    }
}
