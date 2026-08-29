package com.wmp.downloader.newArchitecture.ui.mainPanels;

import com.wmp.downloader.newArchitecture.ParserTaskInfo;
import com.wmp.downloader.newArchitecture.abstractTask.AbstractSpecialSettingsPage;
import com.wmp.downloader.newArchitecture.ui.task.FFmpegSettings;
import com.wmp.downloader.tools.ui.ToastMessage;
import com.wmp.downloader.tools.ui.UITools;
import com.wmp.downloader.ui.Downloader;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Objects;

public class SpecialSettingsPanel {

    private static final Logger logger = Logger.getLogger(SpecialSettingsPanel.class);

    public JPanel specialSettingsPanel;
    private JTabbedPane SpecialSettingsTabbedPane;

    private final Downloader downloader;

    public SpecialSettingsPanel(Downloader downloader) {
        this.downloader = downloader;
    }

    public void initSpecialSettingsComponents() {
        var parserList = ParserTaskInfo.getEnablePluginParserList();
        ArrayList<AbstractSpecialSettingsPage> basicSpecialSettings =
                null;
        try {
            var list = parserList.stream()
                    .map(parser -> {
                        try {
                            return parser.getSettingsPage();
                        } catch (Exception e) {
                            ToastMessage.show(e.getMessage(), ToastMessage.ERROR);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            basicSpecialSettings = new ArrayList<>(list);
        } catch (Exception e) {
            logger.error("发生错误", e);
            ToastMessage.show(e.getMessage(), ToastMessage.ERROR);
        }
        if (basicSpecialSettings != null) {
            basicSpecialSettings.add(new FFmpegSettings());
        }
        for (var specialSettings : basicSpecialSettings) {
            var jScrollPane1 = new JScrollPane(specialSettings);
            UITools.setScrollPaneUnOpaque(jScrollPane1);
            SpecialSettingsTabbedPane.addTab(specialSettings.getSettingsName(), jScrollPane1);
        }
    }

    public JTabbedPane getSpecialSettingsTabbedPane() {
        return SpecialSettingsTabbedPane;
    }
}
