package com.wmp.downloader.newArchitecture.ui.task.http;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractParser;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.AbstractLinkInfoPanel;
import com.wmp.downloader.newArchitecture.abstractTask.linkInfoPanel.LinkFileInfoPanel;
import com.wmp.speed_bump.common.background.tools.SBLogger;

import static com.wmp.downloader.tools.download.URLDownloadTool.extractFileName;
import static com.wmp.downloader.tools.download.URLDownloadTool.getFileSize;

public class HTTPParser extends AbstractParser {

    private static final SBLogger logger = SBLogger.getLogger(HTTPParser.class);

    @Override
    public String getID() {
        return "http";
    }

    @Override
    public String getSupportTip() {
        return "HTTP";
    }

    @Override
    protected void updateLinkInfo(String link) {

    }

    @Override
    protected AbstractLinkInfoPanel getLinkedInfoPanel(String link, Info info) {
        try {
            var fileName = extractFileName(link);
            var fileSizeNum = getFileSize(link);
            return LinkFileInfoPanel
                    .createPanel(fileName, fileSizeNum, "HTTP", link, info);
        } catch (Exception e) {
            logger.error("Error parsing HTTP link: " + link, e);
        }
        return null;
    }

    @Override
    public boolean isMeetRequirements(String link) {
        return link.strip().startsWith("http");
    }

    @Override
    protected AbstractTask getTask(String link, JSONObject infoJson) {
        return new HTTPDownloadTask(infoJson);
    }


    @Override
    public AbstractSpecialSettingsPage getSettingsPage() {
        return null;
    }
}
