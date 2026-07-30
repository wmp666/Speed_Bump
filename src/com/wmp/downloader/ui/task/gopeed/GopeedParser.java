package com.wmp.downloader.ui.task.gopeed;

import com.wmp.downloader.tools.StringFormat;
import com.wmp.downloader.ui.task.Parser;
import com.wmp.downloader.ui.task.createTask.LinkFileInfoPanel;

import javax.swing.*;

public class GopeedParser extends Parser {
    @Override
    public JPanel parse(String content) {
        content = content.strip().substring(9); //开头 gopeed://
        return LinkFileInfoPanel
                .createBasicLinkFileInfoPanel(StringFormat.translate("task", "task.create_task.please_set_file_name"), 0, "gopeed", content);
    }
}
