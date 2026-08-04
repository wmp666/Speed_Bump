package com.wmp.downloader.newArchitecture.abstractTask.downloadTask;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.exception.DownloadException;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.IconControl;
import com.wmp.downloader.tools.ui.ToastMessage;

import javax.swing.*;

public abstract class FolderDownloadTask extends AbstractTask {

    public FolderDownloadTask(JSONObject jsonObject) {
        super(jsonObject);
    }

    @Override
    protected ImageIcon getIcon(int size) {
        return IconControl.getIcon("folder", size);
    }
}
