package com.wmp.downloader.newArchitecture.ui.task.github;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;
import com.wmp.downloader.newArchitecture.ui.task.http.HTTPParser;
import com.wmp.downloader.tools.DataControl;
import com.wmp.downloader.tools.StringFormat;

public class GithubParser extends AbstractParser {
    private final HTTPParser parser = new HTTPParser();
    private Info info;

    @Override
    public String getID() {
        return "github";
    }

    @Override
    public String getSupportTip() {
        return "Github";
    }

    @Override
    protected void updateLinkInfo(String link) {
        if (isMeetRequirements(link)) {
            link = "https://" + DataControl.get("github_accelerate_link", "gh-proxy.org") + "/" + link;
            info = parser.setLink(link);
        }

    }

    @Override
    protected AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info) {
        return this.info.getLinkedInfoPanel();
    }

    @Override
    public boolean isMeetRequirements(String link) {

        return link.strip().startsWith("http") &&
                DataControl.get("is_use_github_accelerate", false) &&
                link.contains("github.com");
    }

    @Override
    protected AbstractTask getTask(String link, JSONObject infoJson) {
        return info.getTask(infoJson);
    }

    @Override
    public AbstractSpecialSettingsPage getSettingsPage() {
        return new GithubAccelerateSettings();
    }
}
