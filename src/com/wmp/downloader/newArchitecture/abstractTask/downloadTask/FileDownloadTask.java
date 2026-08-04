package com.wmp.downloader.newArchitecture.abstractTask.downloadTask;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.exception.DownloadException;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;

import javax.swing.*;

public abstract class FileDownloadTask extends AbstractTask {

    public FileDownloadTask(JSONObject jsonObject) {
        super(jsonObject);
    }

    @Override
    protected ImageIcon getIcon(int size) {
        return super.getIcon(size);
    }
}
