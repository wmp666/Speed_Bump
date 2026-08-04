package com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;

import javax.swing.*;

public abstract class AbstractLinkInfoPanel extends JPanel {

    protected final JSONObject jsonInfo = new JSONObject();

    private final AbstractParser.Info info;
    private final String link;

    public AbstractLinkInfoPanel(AbstractParser.Info info) {
        this.info = info;
        this.link = this.info.getLink();
    }

    public AbstractParser.Info getInfo() {
        return info;
    }

    /**
     * 获取用户选择的任务资源
     *
     * @return 任务资源
     */
    public abstract JSONObject getJsonInfo();
}
