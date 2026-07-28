package com.wmp.downloader.ui.task.bt;

import com.frostwire.jlibtorrent.*;
import com.frostwire.jlibtorrent.swig.error_code;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.MagnetToTorrent;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BTParser extends Parser {



    @Override
    public JPanel parse(String content) {
        if (content.endsWith(".torrent")) {
            File file = new File(content);
            TorrentInfo ti = new TorrentInfo(file);
            //单文件
            if (ti.numFiles() == 1) {
                return LinkFileInfoPanel
                        .createBasicLinkFileInfoPanel(ti.name(), ti.totalSize(), "BT-Torrent", content);
            }
        }else if (content.startsWith("magnet:")){
            var torrentPath = MagnetToTorrent.magnetToTorrent(content, DataControl.getDownloadFilePath().getAbsolutePath(), DataControl.getTempPath().getAbsolutePath());
            File file = new File(torrentPath);
            TorrentInfo ti = new TorrentInfo(file);
            //单文件
            if (ti.numFiles() == 1) {
                return LinkFileInfoPanel
                        .createBasicLinkFileInfoPanel(ti.name(), ti.totalSize(), "BT-Magnet", torrentPath);
            }
        }

        return null;
    }

    public static int getLinkMode(String content) {
        if (content.endsWith(".torrent")) {
            return 0;
        }else if (content.startsWith("magnet:")){
            return 1;
        }else return -1;
    }
}
