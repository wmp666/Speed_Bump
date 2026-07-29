package com.wmp.downloader.ui.task.ed2k;

import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;

public class Ed2kParser extends Parser {
    @Override
    public LinkFileInfoPanel parse(String content) {

        // 1. 去除 "ed2k://|file|" 前缀和末尾的 "|/"
        String processingStr = content.substring("ed2k://|file|".length(), content.length() - 2);

        // 2. 按 "|" 分割
        String[] parts = processingStr.split("\\|");

        if (parts.length >= 3) {
            String fileName = parts[0];
            String fileSize = parts[1]; // 这是字符串形式的字节数

            return LinkFileInfoPanel
                    .createBasicLinkFileInfoPanel(fileName, Long.parseLong(fileSize), "ed2k", content);
        }
        return null;
    }
}
