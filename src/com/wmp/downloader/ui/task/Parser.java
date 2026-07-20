package com.wmp.downloader.ui.task;

import com.wmp.downloader.ui.task.bilibili.BiliParser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;
import com.wmp.downloader.ui.task.ed2k.Ed2kParser;
import com.wmp.downloader.ui.task.http.HTTPParser;

import javax.swing.*;

public abstract class Parser {
    public abstract JPanel parse(String content);


    public static Parser getParser(String url) {
        if(url.startsWith("BV") || url.contains("bilibili.com")){
            return new BiliParser();
        }else if (url.strip().startsWith("http")) {
            return new HTTPParser();
        }
        //else if(url.strip().startsWith("ed2k://")){
        //    return new Ed2kParser();
        //}
        return new Parser() {
            @Override
            public LinkFileInfoPanel parse(String content) {
                return LinkFileInfoPanel
                        .createBasicLinkFileInfoPanel("", 0, "", "");
            }
        };
    }
}
