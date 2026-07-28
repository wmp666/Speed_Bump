package com.wmp.downloader.ui.task.bt;

import com.frostwire.jlibtorrent.TorrentInfo;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;

import javax.swing.*;
import java.io.File;

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
        }

        return null;
    }
}
