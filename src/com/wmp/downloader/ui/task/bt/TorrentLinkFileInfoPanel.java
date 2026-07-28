package com.wmp.downloader.ui.task.bt;

import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;

import java.awt.event.ActionEvent;

public class TorrentLinkFileInfoPanel extends LinkFileInfoPanel {
    public TorrentLinkFileInfoPanel(String name, long size, String mode, String url) {
        super(name, size, mode, url);
    }

    @Override
    public void editButtonAction(ActionEvent e) {

    }
}
