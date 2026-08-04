package com.wmp.downloader.newArchitecture.ui.task.gopeed;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.LinkFileInfoPanel;
import com.wmp.downloader.tools.StringFormat;

public class GopeedParser extends AbstractParser {
    @Override
    public String getID() {
        return "gopeed";
    }

    @Override
    public String getSupportTip() {
        return "gopeed";
    }

    @Override
    protected void updateLinkInfo(String link) {
    }

    @Override
    protected AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info) {
        link = link.strip().substring(9); //开头 gopeed://
        return LinkFileInfoPanel
                .createPanel(
                        StringFormat.translate("task", "task.create_task.please_set_file_name"),
                        0, "gopeed", link, info);
    }

    @Override
    public boolean isMeetRequirements(String link) {
        return link.strip().startsWith("gopeed://");
    }

    @Override
    protected AbstractTask getTask(String link, JSONObject infoJson) {
        return new GopeedDownloadTask(infoJson);
    }

    @Override
    public AbstractSpecialSettingsPage getSettingsPage() {
        return new GopeedSettings();
    }
}
