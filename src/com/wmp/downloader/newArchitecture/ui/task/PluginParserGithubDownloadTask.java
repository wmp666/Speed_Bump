package com.wmp.downloader.newArchitecture.ui.task;

import com.alibaba.fastjson2.JSONObject;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractTask;
import com.wmp.downloader.newArchitecture.ui.task.github.GithubParser;
import com.wmp.downloader.newArchitecture.ui.task.http.HTTPDownloadTask;
import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.tools.ui.ToastMessage;

public class PluginParserGithubDownloadTask extends GithubParser {
    private final Runnable run;

    public PluginParserGithubDownloadTask(Runnable doWhenFinaly) {
        this.run = doWhenFinaly;
    }

    @Override
    protected AbstractTask getTask(String link, JSONObject infoJson) {
        var httpDownloadTask = new HTTPDownloadTask(infoJson) {
            @Override
            public void runWhenFinally() {
                run.run();
            }
        };
        ToastMessage.show(String.format(
                StringFormat.translate("plugins.install.create_success"),
                infoJson.getString("rootName")), ToastMessage.SUCCESS);
        return httpDownloadTask;
    }
}
